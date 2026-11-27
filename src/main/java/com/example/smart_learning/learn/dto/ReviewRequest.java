package com.example.smart_learning.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分最低 1 星")
        @Max(value = 5, message = "评分最高 5 星")
        Integer rating,
        @NotBlank(message = "请填写评价内容")
        @Size(max = 1000, message = "评价不能超过 1000 字")
        String content
) {
}
