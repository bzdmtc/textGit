package com.example.smart_learning.course.dto;

import java.util.List;

public record CourseDetailDto(CourseDto course, List<ChapterDto> chapters) {
}
