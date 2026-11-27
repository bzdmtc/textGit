package com.example.smart_learning.learn.dto;

import jakarta.validation.constraints.Min;

public record ProgressRequest(
        @Min(value = 0, message = "观看时长不能为负数")
        Integer watchedSec,
        Boolean completed
) {
}
