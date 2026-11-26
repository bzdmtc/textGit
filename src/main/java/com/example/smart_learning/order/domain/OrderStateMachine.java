package com.example.smart_learning.order.domain;

import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：所有状态流转集中校验，非法流转直接拒绝。
 * PENDING_PAYMENT -> PAID / CANCELED
 * PAID -> REFUNDING / COMPLETED
 * REFUNDING -> PAID（驳回） / REFUNDED（通过）
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELED));
        TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.REFUNDING, OrderStatus.COMPLETED));
        TRANSITIONS.put(OrderStatus.REFUNDING, EnumSet.of(OrderStatus.PAID, OrderStatus.REFUNDED));
        TRANSITIONS.put(OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {
    }

    public static boolean canTransition(String from, String to) {
        OrderStatus fromStatus = parse(from);
        OrderStatus toStatus = parse(to);
        return fromStatus != null && toStatus != null
                && TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus);
    }

    public static void requireTransition(String from, String to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID,
                    "订单状态不能从 " + from + " 变更为 " + to);
        }
    }

    private static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
