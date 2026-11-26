package com.example.smart_learning.order;

import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.order.domain.OrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    @Test
    void allowsLegalTransitions() {
        assertThat(OrderStateMachine.canTransition("PENDING_PAYMENT", "PAID")).isTrue();
        assertThat(OrderStateMachine.canTransition("PENDING_PAYMENT", "CANCELED")).isTrue();
        assertThat(OrderStateMachine.canTransition("PAID", "REFUNDING")).isTrue();
        assertThat(OrderStateMachine.canTransition("PAID", "COMPLETED")).isTrue();
        assertThat(OrderStateMachine.canTransition("REFUNDING", "PAID")).isTrue();
        assertThat(OrderStateMachine.canTransition("REFUNDING", "REFUNDED")).isTrue();
    }

    @Test
    void rejectsIllegalTransitions() {
        assertThat(OrderStateMachine.canTransition("CANCELED", "PAID")).isFalse();
        assertThat(OrderStateMachine.canTransition("PAID", "PENDING_PAYMENT")).isFalse();
        assertThat(OrderStateMachine.canTransition("REFUNDED", "PAID")).isFalse();
        assertThat(OrderStateMachine.canTransition("UNKNOWN", "PAID")).isFalse();
        assertThatThrownBy(() -> OrderStateMachine.requireTransition("PENDING_PAYMENT", "REFUNDED"))
                .isInstanceOf(BusinessException.class);
    }
}
