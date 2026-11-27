package com.example.smart_learning.learn.dto;

public record LearnLessonDto(
        Long id,
        String title,
        String videoUrl,
        Integer durationSec,
        Boolean isTrial,
        Integer sort,
        Integer watchedSec,
        Boolean completed
) {
}
