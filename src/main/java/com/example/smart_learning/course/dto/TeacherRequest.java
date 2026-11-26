package com.example.smart_learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @NotBlank(message = "讲师姓名不能为空")
        @Size(max = 64, message = "讲师姓名过长")
        String name,
        @Size(max = 64, message = "头衔过长")
        String title,
        @Size(max = 500, message = "头像地址过长")
        String avatar,
        @Size(max = 32, message = "科目过长")
        String subject,
        String introduction,
        Integer sort,
        String status
) {
}
