package com.example.smart_learning.enrollment.dto;

import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.enrollment.entity.UserCourse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyCourseDto(
        Long id,
        Long courseId,
        String courseTitle,
        String coverUrl,
        String subject,
        String stage,
        BigDecimal price,
        LocalDateTime enrolledAt,
        BigDecimal progressPercent,
        String status,
        Long orderId
) {

    public static MyCourseDto of(UserCourse userCourse, Course course) {
        return new MyCourseDto(userCourse.getId(), course.getId(), course.getTitle(), course.getCoverUrl(),
                course.getSubject(), course.getStage(), course.getPrice(), userCourse.getEnrolledAt(),
                userCourse.getProgressPercent(), userCourse.getStatus(), userCourse.getOrderId());
    }
}
