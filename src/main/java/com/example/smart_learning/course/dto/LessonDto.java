package com.example.smart_learning.course.dto;

import com.example.smart_learning.course.entity.CourseLesson;

public record LessonDto(
        Long id,
        Long chapterId,
        String title,
        String videoUrl,
        Integer durationSec,
        Boolean isTrial,
        Integer sort,
        String status
) {

    public static LessonDto of(CourseLesson lesson) {
        return new LessonDto(lesson.getId(), lesson.getChapterId(), lesson.getTitle(), lesson.getVideoUrl(),
                lesson.getDurationSec(), lesson.getIsTrial(), lesson.getSort(), lesson.getStatus());
    }
}
