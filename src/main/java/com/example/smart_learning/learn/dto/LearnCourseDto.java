package com.example.smart_learning.learn.dto;

import java.math.BigDecimal;
import java.util.List;

public record LearnCourseDto(
        Long courseId,
        String title,
        String subtitle,
        String coverUrl,
        String subject,
        String stage,
        BigDecimal progressPercent,
        Long lastLessonId,
        Boolean favorited,
        ReviewDto myReview,
        List<LearnChapterDto> chapters
) {
}
