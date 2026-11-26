package com.example.smart_learning.payment.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.coupon.service.CouponUserService;
import com.example.smart_learning.enrollment.service.EnrollmentService;
import com.example.smart_learning.order.domain.OrderStateMachine;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.repository.OrderRepository;
import com.example.smart_learning.order.service.OrderAssembler;
import com.example.smart_learning.payment.dto.ChannelDto;
import com.example.smart_learning.payment.dto.PaymentResultDto;
import com.example.smart_learning.payment.entity.PaymentRecord;
import com.example.smart_learning.payment.gateway.PaymentChannel;
import com.example.smart_learning.payment.gateway.PaymentResult;
import com.example.smart_learning.payment.repository.PaymentRecordRepository;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Master
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final CouponUserService couponUserService;
    private final EnrollmentService enrollmentService;
    private final OrderAssembler orderAssembler;
    private final Map<String, PaymentChannel> channels = new LinkedHashMap<>();

    public PaymentService(OrderRepository orderRepository,
                          PaymentRecordRepository paymentRecordRepository,
                          CouponUserService couponUserService,
                          EnrollmentService enrollmentService,
                          OrderAssembler orderAssembler,
                          List<PaymentChannel> paymentChannels) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.couponUserService = couponUserService;
        this.enrollmentService = enrollmentService;
        this.orderAssembler = orderAssembler;
        for (PaymentChannel channel : paymentChannels) {
            channels.put(channel.code(), channel);
        }
    }

    public List<ChannelDto> channels() {
        return channels.values().stream().map(channel -> new ChannelDto(channel.code(), channel.displayName())).toList();
    }

    @Transactional
    public PaymentResultDto pay(String orderNo, String channelCode) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Order order = orderRepository.findForUpdateByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 幂等：已支付/已完成订单重复调用直接返回成功结果，不重复写流水
        if ("PAID".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
            PaymentRecord record = paymentRecordRepository
                    .findFirstByOrderNoAndStatusOrderByIdDesc(orderNo, "SUCCESS").orElse(null);
            return new PaymentResultDto(orderAssembler.toDto(order),
                    record == null ? order.getPayMethod() : record.getChannel(),
                    record == null ? null : record.getTransactionNo(),
                    record == null ? order.getPaidAt() : record.getPaidAt(),
                    "订单已支付");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "当前订单状态不可支付");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (order.getExpireAt() != null && now.isAfter(order.getExpireAt())) {
            throw new BusinessException(ErrorCode.ORDER_EXPIRED, "订单已超时关闭，请重新下单");
        }

        PaymentChannel channel = channels.get(channelCode);
        if (channel == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的支付渠道");
        }
        PaymentResult result = channel.pay(order);
        if (!result.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    result.message() == null ? ErrorCode.PAYMENT_FAILED.message() : result.message());
        }

        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(order.getOrderNo());
        record.setChannel(channel.code());
        record.setTransactionNo(result.transactionNo());
        record.setAmount(order.getPayAmount());
        record.setStatus("SUCCESS");
        record.setPaidAt(result.paidAt());
        paymentRecordRepository.save(record);

        OrderStateMachine.requireTransition(order.getStatus(), "PAID");
        order.setStatus("PAID");
        order.setPayMethod(channel.code());
        order.setPaidAt(result.paidAt());
        order = orderRepository.save(order);

        couponUserService.markUsed(order);
        enrollmentService.enroll(order);
        return new PaymentResultDto(orderAssembler.toDto(order), channel.code(),
                result.transactionNo(), result.paidAt(), "支付成功，课程已开通");
    }
}
