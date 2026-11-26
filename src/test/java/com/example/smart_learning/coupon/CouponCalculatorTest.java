package com.example.smart_learning.coupon;

import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.service.CouponCalculator;
import com.example.smart_learning.course.entity.Course;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CouponCalculatorTest {

    @Test
    void fixedCouponDiscount() {
        Coupon coupon = coupon("FIXED", "50.00", "500.00");
        assertThat(CouponCalculator.discount(coupon, new BigDecimal("599.00")))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void fixedCouponNeverExceedsTotal() {
        Coupon coupon = coupon("FIXED", "50.00", "0.00");
        assertThat(CouponCalculator.discount(coupon, new BigDecimal("30.00")))
                .isEqualByComparingTo("30.00");
    }

    @Test
    void percentCouponRepresentsPayRate() {
        Coupon coupon = coupon("PERCENT", "90.00", "0.00");
        assertThat(CouponCalculator.discount(coupon, new BigDecimal("699.00")))
                .isEqualByComparingTo("69.90");
    }

    @Test
    void subjectMustMatchAllCourses() {
        Coupon coupon = coupon("FIXED", "30.00", "0.00");
        coupon.setSubject("数学一");
        Course math = new Course();
        math.setSubject("数学一");
        Course english = new Course();
        english.setSubject("英语一");
        assertThat(CouponCalculator.matchesSubject(coupon, List.of(math))).isTrue();
        assertThat(CouponCalculator.matchesSubject(coupon, List.of(math, english))).isFalse();
    }

    @Test
    void claimableRequiresActiveWindowAndStock() {
        Coupon coupon = coupon("FIXED", "10.00", "0.00");
        LocalDateTime now = LocalDateTime.of(2026, 11, 26, 12, 0);
        coupon.setStartAt(now.minusDays(1));
        coupon.setEndAt(now.plusDays(1));
        coupon.setRemainingQuantity(1);
        assertThat(CouponCalculator.isClaimable(coupon, now)).isTrue();

        coupon.setRemainingQuantity(0);
        assertThat(CouponCalculator.isClaimable(coupon, now)).isFalse();

        coupon.setRemainingQuantity(1);
        coupon.setStatus("OFF");
        assertThat(CouponCalculator.isClaimable(coupon, now)).isFalse();
    }

    private Coupon coupon(String type, String value, String minAmount) {
        Coupon coupon = new Coupon();
        coupon.setType(type);
        coupon.setValueAmount(new BigDecimal(value));
        coupon.setMinAmount(new BigDecimal(minAmount));
        coupon.setTotalQuantity(100);
        coupon.setRemainingQuantity(100);
        coupon.setPerUserLimit(1);
        coupon.setStatus("ON");
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        return coupon;
    }
}
