package com.example.smart_learning.course.dto;

import com.example.smart_learning.course.entity.CourseChapter;

import java.util.List;

public record ChapterDto(
        Long id,
        Long courseId,
        String title,
        Integer sort,
        List<LessonDto> lessons
) {

    public static ChapterDto of(CourseChapter chapter, List<LessonDto> lessons) {
        return new ChapterDto(chapter.getId(), chapter.getCourseId(), chapter.getTitle(), chapter.getSort(), lessons);
    }
}
