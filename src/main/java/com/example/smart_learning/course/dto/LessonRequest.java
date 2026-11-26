package com.example.smart_learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank(message = "课时标题不能为空")
        @Size(max = 200, message = "课时标题过长")
        String title,
        @Size(max = 500, message = "视频地址过长")
        String videoUrl,
        Integer durationSec,
        Boolean isTrial,
        Integer sort,
        String status
) {
}
