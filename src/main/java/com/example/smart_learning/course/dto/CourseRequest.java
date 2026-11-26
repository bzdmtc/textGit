package com.example.smart_learning.course.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CourseRequest(
        @NotBlank(message = "课程标题不能为空")
        @Size(max = 200, message = "课程标题过长")
        String title,
        @Size(max = 255, message = "副标题过长")
        String subtitle,
        @NotBlank(message = "科目不能为空")
        @Size(max = 32, message = "科目过长")
        String subject,
        @NotBlank(message = "阶段不能为空")
        @Size(max = 32, message = "阶段过长")
        String stage,
        @Size(max = 500, message = "封面地址过长")
        String coverUrl,
        String description,
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.00", message = "价格不能为负")
        BigDecimal price,
        @DecimalMin(value = "0.00", message = "原价不能为负")
        BigDecimal originalPrice,
        String status,
        List<Long> teacherIds
) {
}
