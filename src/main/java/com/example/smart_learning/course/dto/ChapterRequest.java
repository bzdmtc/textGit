package com.example.smart_learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotBlank(message = "章节标题不能为空")
        @Size(max = 200, message = "章节标题过长")
        String title,
        Integer sort
) {
}
