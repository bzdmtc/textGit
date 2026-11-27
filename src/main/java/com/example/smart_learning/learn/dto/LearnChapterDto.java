package com.example.smart_learning.learn.dto;

import java.util.List;

public record LearnChapterDto(
        Long id,
        String title,
        Integer sort,
        List<LearnLessonDto> lessons
) {
}
