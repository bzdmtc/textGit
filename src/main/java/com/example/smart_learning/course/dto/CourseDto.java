package com.example.smart_learning.course.dto;

import com.example.smart_learning.course.entity.Course;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CourseDto(
        Long id,
        String title,
        String subtitle,
        String subject,
        String stage,
        String coverUrl,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        BigDecimal rating,
        Integer studentCount,
        String status,
        Long ownerUserId,
        LocalDateTime createdAt,
        List<TeacherDto> teachers
) {

    public static CourseDto of(Course course, List<TeacherDto> teachers) {
        return new CourseDto(course.getId(), course.getTitle(), course.getSubtitle(), course.getSubject(),
                course.getStage(), course.getCoverUrl(), course.getDescription(), course.getPrice(),
                course.getOriginalPrice(), course.getRating(), course.getStudentCount(), course.getStatus(),
                course.getOwnerUserId(), course.getCreatedAt(), teachers);
    }
}
