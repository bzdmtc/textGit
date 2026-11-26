package com.example.smart_learning.order.service;

import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.entity.UserCoupon;
import com.example.smart_learning.coupon.repository.CouponRepository;
import com.example.smart_learning.coupon.repository.UserCouponRepository;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.dto.OrderItemDto;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.entity.OrderItem;
import com.example.smart_learning.order.entity.RefundRequest;
import com.example.smart_learning.order.repository.OrderItemRepository;
import com.example.smart_learning.order.repository.RefundRequestRepository;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderAssembler {

    private final OrderItemRepository orderItemRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final SysUserRepository userRepository;

    public OrderAssembler(OrderItemRepository orderItemRepository,
                          RefundRequestRepository refundRequestRepository,
                          UserCouponRepository userCouponRepository,
                          CouponRepository couponRepository,
                          SysUserRepository userRepository) {
        this.orderItemRepository = orderItemRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.userCouponRepository = userCouponRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
    }

    public OrderDto toDto(Order order) {
        return toDtos(List.of(order)).get(0);
    }

    public List<OrderDto> toDtos(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        Map<Long, List<OrderItemDto>> itemsByOrder = orderItemRepository
                .findByOrderIdInOrderByIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, LinkedHashMap::new,
                        Collectors.mapping(OrderItemDto::of, Collectors.toList())));

        Map<Long, RefundRequest> latestRefunds = new HashMap<>();
        for (RefundRequest refund : refundRequestRepository.findByOrderIdInOrderByIdDesc(orderIds)) {
            latestRefunds.putIfAbsent(refund.getOrderId(), refund);
        }

        Map<Long, String> couponNames = couponNames(orders);
        Map<Long, SysUser> users = userRepository.findAllById(
                        orders.stream().map(Order::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<OrderDto> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            RefundRequest refund = latestRefunds.get(order.getId());
            SysUser user = users.get(order.getUserId());
            String couponName = order.getUserCouponId() == null ? null : couponNames.get(order.getUserCouponId());
            OrderDto dto = OrderDto.of(order,
                    itemsByOrder.getOrDefault(order.getId(), List.of()),
                    couponName,
                    refund == null ? null : refund.getStatus(),
                    refund == null ? null : refund.getReason());
            result.add(user == null ? dto : dto.withUser(user.getAccount(), displayName(user)));
        }
        return result;
    }

    private Map<Long, String> couponNames(List<Order> orders) {
        List<Long> userCouponIds = orders.stream()
                .map(Order::getUserCouponId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (userCouponIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, UserCoupon> userCoupons = userCouponRepository.findAllById(userCouponIds).stream()
                .collect(Collectors.toMap(UserCoupon::getId, Function.identity()));
        List<Long> couponIds = userCoupons.values().stream().map(UserCoupon::getCouponId).distinct().toList();
        Map<Long, Coupon> coupons = couponRepository.findByIdIn(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        Map<Long, String> result = new HashMap<>();
        userCoupons.forEach((id, userCoupon) -> {
            Coupon coupon = coupons.get(userCoupon.getCouponId());
            if (coupon != null) {
                result.put(id, coupon.getName());
            }
        });
        return result;
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
