package com.example.smart_learning.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank(message = "优惠券名称不能为空")
        @Size(max = 100, message = "优惠券名称过长")
        String name,
        @NotBlank(message = "优惠券类型不能为空")
        String type,
        @NotNull(message = "优惠值不能为空")
        @DecimalMin(value = "0.01", message = "优惠值必须大于 0")
        BigDecimal value,
        @DecimalMin(value = "0.00", message = "最低消费不能为负")
        BigDecimal minAmount,
        @NotNull(message = "发放数量不能为空")
        @Min(value = 1, message = "发放数量至少为 1")
        Integer totalQuantity,
        @Min(value = 1, message = "每人限领至少为 1")
        Integer perUserLimit,
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startAt,
        @NotNull(message = "结束时间不能为空")
        LocalDateTime endAt,
        @Size(max = 32, message = "适用科目过长")
        String subject,
        @Size(max = 500, message = "说明过长")
        String description,
        String status
) {
}
