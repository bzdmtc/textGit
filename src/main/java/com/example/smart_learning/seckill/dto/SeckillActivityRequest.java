package com.example.smart_learning.seckill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 时间字段使用 UTC 墙上时间（与库内 DATETIME2 一致） */
public record SeckillActivityRequest(
        @NotNull(message = "请选择课程")
        Long courseId,
        @NotNull(message = "秒杀价不能为空")
        @DecimalMin(value = "0.01", message = "秒杀价必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "秒杀价格式不正确")
        BigDecimal seckillPrice,
        @NotNull(message = "总名额不能为空")
        @Min(value = 1, message = "总名额至少为 1")
        Integer totalStock,
        @NotNull(message = "每人限购不能为空")
        @Min(value = 1, message = "每人限购至少为 1")
        Integer perUserLimit,
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startAt,
        @NotNull(message = "结束时间不能为空")
        LocalDateTime endAt,
        @Pattern(regexp = "ON|OFF", message = "状态只能为 ON 或 OFF")
        String status
) {
}
