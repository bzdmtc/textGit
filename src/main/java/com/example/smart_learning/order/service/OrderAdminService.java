package com.example.smart_learning.order.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.enrollment.service.EnrollmentService;
import com.example.smart_learning.order.domain.OrderStateMachine;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.dto.RefundDto;
import com.example.smart_learning.order.dto.RefundHandleRequest;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.entity.RefundRequest;
import com.example.smart_learning.order.repository.OrderRepository;
import com.example.smart_learning.order.repository.RefundRequestRepository;
import com.example.smart_learning.payment.entity.PaymentRecord;
import com.example.smart_learning.payment.repository.PaymentRecordRepository;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.security.SecurityUtils;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SysUserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final AuditService auditService;
    private final OrderAssembler orderAssembler;

    public OrderAdminService(OrderRepository orderRepository,
                             RefundRequestRepository refundRequestRepository,
                             PaymentRecordRepository paymentRecordRepository,
                             SysUserRepository userRepository,
                             EnrollmentService enrollmentService,
                             AuditService auditService,
                             OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userRepository = userRepository;
        this.enrollmentService = enrollmentService;
        this.auditService = auditService;
        this.orderAssembler = orderAssembler;
    }

    @Transactional(readOnly = true)
    public PageResult<OrderDto> page(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Order> result = orderRepository.searchAdmin(keyword, status, pageable);
        return PageResult.of(orderAssembler.toDtos(result.getContent()),
                result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public OrderDto detail(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return orderAssembler.toDto(order);
    }

    @Transactional(readOnly = true)
    public PageResult<RefundDto> refundPage(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<RefundRequest> result = refundRequestRepository.searchAdmin(keyword, status, pageable);
        List<RefundRequest> refunds = result.getContent();
        if (refunds.isEmpty()) {
            return PageResult.of(List.of(), result.getTotalElements(), page, size);
        }
        List<Long> orderIds = refunds.stream().map(RefundRequest::getOrderId).distinct().toList();
        Map<Long, Order> orders = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, SysUser> users = userRepository.findAllById(
                        refunds.stream().map(RefundRequest::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<RefundDto> items = refunds.stream().map(refund -> {
            SysUser user = users.get(refund.getUserId());
            Order order = orders.get(refund.getOrderId());
            return RefundDto.of(refund,
                    user == null ? null : user.getAccount(),
                    user == null ? null : displayName(user),
                    order == null ? null : order.getStatus());
        }).toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional
    public RefundDto approve(Long refundId, RefundHandleRequest request) {
        RefundRequest refund = refundRequestRepository.findForUpdateById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_HANDLED);
        }
        Order order = orderRepository.findForUpdateById(refund.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderStateMachine.requireTransition(order.getStatus(), "REFUNDED");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        order.setStatus("REFUNDED");
        order = orderRepository.save(order);

        refund.setStatus("APPROVED");
        refund.setAdminNote(request == null ? null : request.adminNote());
        refund.setHandledBy(SecurityUtils.currentUserId());
        refund.setHandledAt(now);
        refund = refundRequestRepository.save(refund);

        List<PaymentRecord> records = paymentRecordRepository.findByOrderNoOrderByIdDesc(order.getOrderNo());
        for (PaymentRecord record : records) {
            if ("SUCCESS".equals(record.getStatus())) {
                record.setStatus("REFUNDED");
                paymentRecordRepository.save(record);
            }
        }
        enrollmentService.revoke(order);
        auditService.record("REFUND_APPROVE", "REFUND", refund.getId(), null, refund.getAdminNote());
        return RefundDto.of(refund, null, null, order.getStatus());
    }

    @Transactional
    public RefundDto reject(Long refundId, RefundHandleRequest request) {
        RefundRequest refund = refundRequestRepository.findForUpdateById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_HANDLED);
        }
        Order order = orderRepository.findForUpdateById(refund.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderStateMachine.requireTransition(order.getStatus(), "PAID");
        order.setStatus("PAID");
        order = orderRepository.save(order);

        refund.setStatus("REJECTED");
        refund.setAdminNote(request == null ? null : request.adminNote());
        refund.setHandledBy(SecurityUtils.currentUserId());
        refund.setHandledAt(LocalDateTime.now(ZoneOffset.UTC));
        refund = refundRequestRepository.save(refund);
        auditService.record("REFUND_REJECT", "REFUND", refund.getId(), null, refund.getAdminNote());
        return RefundDto.of(refund, null, null, order.getStatus());
    }

    private String displayName(SysUser user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getRealname() != null && !user.getRealname().isBlank()) {
            return user.getRealname();
        }
        return user.getAccount();
    }
}
