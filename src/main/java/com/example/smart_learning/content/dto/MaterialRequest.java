package com.example.smart_learning.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @NotBlank(message = "资料标题不能为空")
        @Size(max = 200, message = "资料标题过长")
        String title,
        @Size(max = 32, message = "科目过长")
        String subject,
        @NotBlank(message = "文件地址不能为空")
        @Size(max = 500, message = "文件地址过长")
        String fileUrl,
        Long fileSize,
        String status
) {
}
