package com.example.smart_learning.content.dto;

import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.university.dto.UniversityDto;

import java.util.List;

public record HomeDto(
        List<BannerDto> banners,
        List<CourseDto> hotCourses,
        List<TeacherDto> teachers,
        List<ArticleDto> latestArticles,
        List<UniversityDto> universities
) {
}
