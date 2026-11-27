package com.example.smart_learning.learn.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.CourseChapter;
import com.example.smart_learning.course.entity.CourseLesson;
import com.example.smart_learning.course.repository.CourseChapterRepository;
import com.example.smart_learning.course.repository.CourseLessonRepository;
import com.example.smart_learning.enrollment.entity.UserCourse;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 学习相关访问校验：必须登录且已购买课程。 */
@Service
@Master
public class LearnAccessService {

    private final UserCourseRepository userCourseRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseLessonRepository lessonRepository;

    public LearnAccessService(UserCourseRepository userCourseRepository,
                              CourseChapterRepository chapterRepository,
                              CourseLessonRepository lessonRepository) {
        this.userCourseRepository = userCourseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
    }

    public Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    @Transactional(readOnly = true)
    public UserCourse requireEnrolled(Long userId, Long courseId) {
        UserCourse enrollment = userCourseRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_ENROLLED));
        if (!"ACTIVE".equals(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }
        return enrollment;
    }

    @Transactional(readOnly = true)
    public List<CourseLesson> courseLessons(Long courseId) {
        List<Long> chapterIds = chapterRepository.findByCourseIdOrderBySortAscIdAsc(courseId).stream()
                .map(CourseChapter::getId)
                .toList();
        if (chapterIds.isEmpty()) {
            return List.of();
        }
        return lessonRepository.findByChapterIdInOrderBySortAscIdAsc(chapterIds).stream()
                .filter(lesson -> "ACTIVE".equals(lesson.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseLesson requireLesson(Long courseId, Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        CourseChapter chapter = chapterRepository.findById(lesson.getChapterId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(ErrorCode.LESSON_NOT_FOUND);
        }
        return lesson;
    }
}
