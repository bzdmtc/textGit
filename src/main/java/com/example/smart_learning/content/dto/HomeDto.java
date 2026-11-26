package com.example.smart_learning.content.dto;

import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.TeacherDto;

import java.util.List;

public record HomeDto(
        List<BannerDto> banners,
        List<CourseDto> hotCourses,
        List<TeacherDto> teachers,
        List<ArticleDto> latestArticles
) {
}
