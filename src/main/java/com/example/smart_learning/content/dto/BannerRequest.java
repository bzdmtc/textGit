package com.example.smart_learning.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BannerRequest(
        @NotBlank(message = "Banner 标题不能为空")
        @Size(max = 128, message = "标题过长")
        String title,
        @NotBlank(message = "图片地址不能为空")
        @Size(max = 500, message = "图片地址过长")
        String imageUrl,
        @Size(max = 500, message = "链接过长")
        String linkUrl,
        Integer sort,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
