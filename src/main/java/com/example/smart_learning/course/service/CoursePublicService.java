package com.example.smart_learning.course.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.ReadOnly;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.dto.ChapterDto;
import com.example.smart_learning.course.dto.CourseDetailDto;
import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.LessonDto;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.entity.CourseChapter;
import com.example.smart_learning.course.entity.CourseTeacher;
import com.example.smart_learning.course.entity.Teacher;
import com.example.smart_learning.course.repository.CourseChapterRepository;
import com.example.smart_learning.course.repository.CourseLessonRepository;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.course.repository.CourseTeacherRepository;
import com.example.smart_learning.course.repository.TeacherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CoursePublicService {

    private final CourseRepository courseRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final TeacherRepository teacherRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseLessonRepository lessonRepository;

    public CoursePublicService(CourseRepository courseRepository,
                               CourseTeacherRepository courseTeacherRepository,
                               TeacherRepository teacherRepository,
                               CourseChapterRepository chapterRepository,
                               CourseLessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.teacherRepository = teacherRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public PageResult<CourseDto> page(String keyword, String subject, String stage, int page, int size) {
        Page<Course> result = courseRepository.search(keyword, "PUBLISHED", subject, stage, null,
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)));
        return PageResult.of(toDtos(result.getContent()), result.getTotalElements(), page, size);
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public CourseDetailDto detail(Long id) {
        Course course = courseRepository.findByIdAndStatus(id, "PUBLISHED")
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程不存在或未上架"));
        return new CourseDetailDto(toDto(course), chapterDtos(course.getId()));
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<CourseDto> hot() {
        return toDtos(courseRepository.findTop8ByStatusOrderByIdDesc("PUBLISHED"));
    }

    private CourseDto toDto(Course course) {
        List<CourseTeacher> links = courseTeacherRepository.findByCourseIdOrderBySortAscIdAsc(course.getId());
        List<Long> teacherIds = links.stream().map(CourseTeacher::getTeacherId).toList();
        Map<Long, Teacher> teacherMap = teacherRepository.findByIdIn(teacherIds).stream()
                .collect(Collectors.toMap(Teacher::getId, Function.identity()));
        List<TeacherDto> teachers = links.stream()
                .map(link -> teacherMap.get(link.getTeacherId()))
                .filter(java.util.Objects::nonNull)
                .map(TeacherDto::of)
                .toList();
        return CourseDto.of(course, teachers);
    }

    private List<CourseDto> toDtos(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<CourseTeacher> links = courseTeacherRepository.findByCourseIdIn(courseIds);
        Set<Long> teacherIds = links.stream().map(CourseTeacher::getTeacherId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, Teacher> teacherMap = teacherRepository.findByIdIn(new ArrayList<>(teacherIds)).stream()
                .collect(Collectors.toMap(Teacher::getId, Function.identity()));
        Map<Long, List<TeacherDto>> byCourse = new LinkedHashMap<>();
        for (CourseTeacher link : links) {
            Teacher teacher = teacherMap.get(link.getTeacherId());
            if (teacher != null) {
                byCourse.computeIfAbsent(link.getCourseId(), key -> new ArrayList<>()).add(TeacherDto.of(teacher));
            }
        }
        return courses.stream().map(course -> CourseDto.of(course, byCourse.getOrDefault(course.getId(), List.of()))).toList();
    }

    private List<ChapterDto> chapterDtos(Long courseId) {
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderBySortAscIdAsc(courseId);
        if (chapters.isEmpty()) {
            return List.of();
        }
        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();
        Map<Long, List<LessonDto>> lessons = lessonRepository.findByChapterIdInOrderBySortAscIdAsc(chapterIds)
                .stream()
                .map(LessonDto::of)
                .collect(Collectors.groupingBy(LessonDto::chapterId, LinkedHashMap::new, Collectors.toList()));
        return chapters.stream()
                .map(chapter -> ChapterDto.of(chapter, lessons.getOrDefault(chapter.getId(), List.of())))
                .toList();
    }
}
