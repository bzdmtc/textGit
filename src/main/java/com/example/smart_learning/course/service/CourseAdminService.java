package com.example.smart_learning.course.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.dto.ChapterDto;
import com.example.smart_learning.course.dto.ChapterRequest;
import com.example.smart_learning.course.dto.CourseDetailDto;
import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.CourseRequest;
import com.example.smart_learning.course.dto.LessonDto;
import com.example.smart_learning.course.dto.LessonRequest;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.entity.CourseChapter;
import com.example.smart_learning.course.entity.CourseLesson;
import com.example.smart_learning.course.entity.CourseTeacher;
import com.example.smart_learning.course.entity.Teacher;
import com.example.smart_learning.course.repository.CourseChapterRepository;
import com.example.smart_learning.course.repository.CourseLessonRepository;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.course.repository.CourseTeacherRepository;
import com.example.smart_learning.course.repository.TeacherRepository;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.permission.service.DataScopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseLessonRepository lessonRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;

    public CourseAdminService(CourseRepository courseRepository,
                              TeacherRepository teacherRepository,
                              CourseTeacherRepository courseTeacherRepository,
                              CourseChapterRepository chapterRepository,
                              CourseLessonRepository lessonRepository,
                              DataScopeService dataScopeService,
                              AuditService auditService) {
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<CourseDto> page(String keyword, String status, String subject, String stage,
                                      int page, int size) {
        Long ownerId = dataScopeService.isAllScope() ? null : dataScopeService.currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Course> result = courseRepository.search(keyword, status, subject, stage, ownerId, pageable);
        return PageResult.of(toDtos(result.getContent()), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public CourseDetailDto detail(Long id) {
        Course course = requireCourse(id);
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        List<ChapterDto> chapters = chapterDtos(course.getId());
        return new CourseDetailDto(toDto(course), chapters);
    }

    @Transactional
    public CourseDto create(CourseRequest request) {
        Course course = new Course();
        course.setOwnerUserId(dataScopeService.currentUserId());
        apply(course, request);
        course = courseRepository.save(course);
        replaceTeachers(course.getId(), request.teacherIds());
        auditService.record("COURSE_CREATE", "COURSE", course.getId(), null, toDto(course));
        return toDto(course);
    }

    @Transactional
    public CourseDto update(Long id, CourseRequest request) {
        Course course = requireCourse(id);
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        CourseDto before = toDto(course);
        apply(course, request);
        course = courseRepository.save(course);
        replaceTeachers(course.getId(), request.teacherIds());
        auditService.record("COURSE_UPDATE", "COURSE", id, before, toDto(course));
        return toDto(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = requireCourse(id);
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        CourseDto before = toDto(course);
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderBySortAscIdAsc(id);
        for (CourseChapter chapter : chapters) {
            lessonRepository.deleteByChapterId(chapter.getId());
        }
        chapterRepository.deleteByCourseId(id);
        courseTeacherRepository.deleteByCourseId(id);
        courseRepository.delete(course);
        auditService.record("COURSE_DELETE", "COURSE", id, before, null);
    }

    @Transactional
    public CourseDto publish(Long id, String status) {
        if (!"PUBLISHED".equals(status) && !"DRAFT".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 PUBLISHED/DRAFT");
        }
        Course course = requireCourse(id);
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        CourseDto before = toDto(course);
        course.setStatus(status);
        course = courseRepository.save(course);
        auditService.record("COURSE_PUBLISH", "COURSE", id, before, toDto(course));
        return toDto(course);
    }

    @Transactional
    public ChapterDto addChapter(Long courseId, ChapterRequest request) {
        Course course = requireCourse(courseId);
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(request.title().trim());
        chapter.setSort(request.sort() == null ? nextChapterSort(courseId) : request.sort());
        chapter = chapterRepository.save(chapter);
        auditService.record("CHAPTER_CREATE", "COURSE", courseId, null, chapter.getTitle());
        return ChapterDto.of(chapter, List.of());
    }

    @Transactional
    public ChapterDto updateChapter(Long chapterId, ChapterRequest request) {
        CourseChapter chapter = requireChapter(chapterId);
        Course course = requireCourse(chapter.getCourseId());
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        chapter.setTitle(request.title().trim());
        if (request.sort() != null) {
            chapter.setSort(request.sort());
        }
        chapter = chapterRepository.save(chapter);
        return ChapterDto.of(chapter, lessonDtos(chapterId));
    }

    @Transactional
    public void deleteChapter(Long chapterId) {
        CourseChapter chapter = requireChapter(chapterId);
        Course course = requireCourse(chapter.getCourseId());
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        lessonRepository.deleteByChapterId(chapterId);
        chapterRepository.delete(chapter);
        auditService.record("CHAPTER_DELETE", "COURSE", course.getId(), chapter.getTitle(), null);
    }

    @Transactional
    public LessonDto addLesson(Long chapterId, LessonRequest request) {
        CourseChapter chapter = requireChapter(chapterId);
        Course course = requireCourse(chapter.getCourseId());
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        CourseLesson lesson = new CourseLesson();
        lesson.setChapterId(chapterId);
        applyLesson(lesson, request);
        lesson = lessonRepository.save(lesson);
        auditService.record("LESSON_CREATE", "COURSE", course.getId(), null, lesson.getTitle());
        return LessonDto.of(lesson);
    }

    @Transactional
    public LessonDto updateLesson(Long lessonId, LessonRequest request) {
        CourseLesson lesson = requireLesson(lessonId);
        CourseChapter chapter = requireChapter(lesson.getChapterId());
        Course course = requireCourse(chapter.getCourseId());
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        applyLesson(lesson, request);
        lesson = lessonRepository.save(lesson);
        return LessonDto.of(lesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        CourseLesson lesson = requireLesson(lessonId);
        CourseChapter chapter = requireChapter(lesson.getChapterId());
        Course course = requireCourse(chapter.getCourseId());
        dataScopeService.assertOwnerAccess(course.getOwnerUserId());
        lessonRepository.delete(lesson);
        auditService.record("LESSON_DELETE", "COURSE", course.getId(), lesson.getTitle(), null);
    }

    private void apply(Course course, CourseRequest request) {
        course.setTitle(request.title().trim());
        course.setSubtitle(request.subtitle());
        course.setSubject(request.subject().trim());
        course.setStage(request.stage().trim());
        course.setCoverUrl(request.coverUrl());
        course.setDescription(request.description());
        course.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        course.setOriginalPrice(request.originalPrice());
        course.setStatus(request.status() == null || request.status().isBlank() ? "DRAFT" : request.status());
    }

    private void applyLesson(CourseLesson lesson, LessonRequest request) {
        lesson.setTitle(request.title().trim());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationSec(request.durationSec() == null ? 0 : request.durationSec());
        lesson.setIsTrial(Boolean.TRUE.equals(request.isTrial()));
        lesson.setSort(request.sort() == null ? 0 : request.sort());
        lesson.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }

    private void replaceTeachers(Long courseId, List<Long> teacherIds) {
        List<Long> distinct = teacherIds == null ? List.of() : teacherIds.stream().distinct().toList();
        if (!distinct.isEmpty()) {
            List<Teacher> teachers = teacherRepository.findByIdIn(distinct);
            if (teachers.size() != distinct.size()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "存在不存在的讲师");
            }
        }
        courseTeacherRepository.deleteByCourseId(courseId);
        courseTeacherRepository.flush();
        int sort = 1;
        for (Long teacherId : distinct) {
            CourseTeacher relation = new CourseTeacher();
            relation.setCourseId(courseId);
            relation.setTeacherId(teacherId);
            relation.setSort(sort++);
            courseTeacherRepository.save(relation);
        }
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

    private List<LessonDto> lessonDtos(Long chapterId) {
        return lessonRepository.findByChapterIdOrderBySortAscIdAsc(chapterId).stream().map(LessonDto::of).toList();
    }

    private int nextChapterSort(Long courseId) {
        return chapterRepository.findByCourseIdOrderBySortAscIdAsc(courseId).stream()
                .mapToInt(CourseChapter::getSort).max().orElse(0) + 1;
    }

    private Course requireCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程不存在"));
    }

    private CourseChapter requireChapter(Long id) {
        return chapterRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "章节不存在"));
    }

    private CourseLesson requireLesson(Long id) {
        return lessonRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课时不存在"));
    }
}
