package com.example.smart_learning.learn.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.entity.CourseChapter;
import com.example.smart_learning.course.entity.CourseLesson;
import com.example.smart_learning.course.repository.CourseChapterRepository;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.entity.UserCourse;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.learn.dto.LearnChapterDto;
import com.example.smart_learning.learn.dto.LearnCourseDto;
import com.example.smart_learning.learn.dto.LearnLessonDto;
import com.example.smart_learning.learn.dto.ProgressDto;
import com.example.smart_learning.learn.dto.ProgressRequest;
import com.example.smart_learning.learn.entity.LessonProgress;
import com.example.smart_learning.learn.repository.CourseFavoriteRepository;
import com.example.smart_learning.learn.repository.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class LearnService {

    private final LearnAccessService accessService;
    private final CourseRepository courseRepository;
    private final CourseChapterRepository chapterRepository;
    private final UserCourseRepository userCourseRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseFavoriteRepository favoriteRepository;
    private final ReviewService reviewService;

    public LearnService(LearnAccessService accessService,
                        CourseRepository courseRepository,
                        CourseChapterRepository chapterRepository,
                        UserCourseRepository userCourseRepository,
                        LessonProgressRepository progressRepository,
                        CourseFavoriteRepository favoriteRepository,
                        ReviewService reviewService) {
        this.accessService = accessService;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.userCourseRepository = userCourseRepository;
        this.progressRepository = progressRepository;
        this.favoriteRepository = favoriteRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public LearnCourseDto courseDetail(Long courseId) {
        Long userId = accessService.requireUserId();
        UserCourse enrollment = accessService.requireEnrolled(userId, courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程不存在"));
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderBySortAscIdAsc(courseId);
        List<CourseLesson> lessons = accessService.courseLessons(courseId);
        List<Long> lessonIds = lessons.stream().map(CourseLesson::getId).toList();
        Map<Long, LessonProgress> progressMap = lessonIds.isEmpty() ? Map.of()
                : progressRepository.findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, Function.identity()));

        Map<Long, List<CourseLesson>> lessonsByChapter = lessons.stream()
                .collect(Collectors.groupingBy(CourseLesson::getChapterId, LinkedHashMap::new, Collectors.toList()));
        List<LearnChapterDto> chapterDtos = chapters.stream()
                .map(chapter -> new LearnChapterDto(chapter.getId(), chapter.getTitle(), chapter.getSort(),
                        lessonsByChapter.getOrDefault(chapter.getId(), List.of()).stream()
                                .map(lesson -> toLessonDto(lesson, progressMap.get(lesson.getId())))
                                .toList()))
                .toList();

        Long lastLessonId = lessonIds.isEmpty() ? null
                : progressRepository.findTopByUserIdAndLessonIdInOrderByUpdatedAtDesc(userId, lessonIds)
                .map(LessonProgress::getLessonId)
                .orElse(lessonIds.get(0));

        return new LearnCourseDto(course.getId(), course.getTitle(), course.getSubtitle(), course.getCoverUrl(),
                course.getSubject(), course.getStage(), enrollment.getProgressPercent(), lastLessonId,
                favoriteRepository.existsByUserIdAndCourseId(userId, courseId),
                reviewService.myReview(userId, courseId), chapterDtos);
    }

    @Transactional
    public ProgressDto reportProgress(Long courseId, Long lessonId, ProgressRequest request) {
        Long userId = accessService.requireUserId();
        UserCourse enrollment = accessService.requireEnrolled(userId, courseId);
        CourseLesson lesson = accessService.requireLesson(courseId, lessonId);

        int duration = lesson.getDurationSec() == null ? 0 : lesson.getDurationSec();
        int watched = request.watchedSec() == null ? 0 : Math.max(0, request.watchedSec());
        if (duration > 0) {
            watched = Math.min(watched, duration);
        }
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(LessonProgress::new);
        progress.setUserId(userId);
        progress.setLessonId(lessonId);
        progress.setWatchedSec(Math.max(progress.getWatchedSec() == null ? 0 : progress.getWatchedSec(), watched));
        boolean autoCompleted = duration > 0 && watched >= (int) Math.ceil(duration * 0.95);
        boolean completed = Boolean.TRUE.equals(progress.getCompleted())
                || Boolean.TRUE.equals(request.completed())
                || autoCompleted;
        progress.setCompleted(completed);
        progress = progressRepository.save(progress);

        BigDecimal percent = recomputeCourseProgress(userId, courseId, enrollment);
        return new ProgressDto(lessonId, progress.getWatchedSec(), progress.getCompleted(), percent);
    }

    private BigDecimal recomputeCourseProgress(Long userId, Long courseId, UserCourse enrollment) {
        List<CourseLesson> lessons = accessService.courseLessons(courseId);
        BigDecimal percent = BigDecimal.ZERO;
        if (!lessons.isEmpty()) {
            List<Long> lessonIds = lessons.stream().map(CourseLesson::getId).toList();
            long completed = progressRepository.countByUserIdAndLessonIdInAndCompletedTrue(userId, lessonIds);
            percent = BigDecimal.valueOf(completed * 100.0 / lessons.size())
                    .setScale(2, RoundingMode.HALF_UP);
        }
        enrollment.setProgressPercent(percent);
        userCourseRepository.save(enrollment);
        return percent;
    }

    private LearnLessonDto toLessonDto(CourseLesson lesson, LessonProgress progress) {
        return new LearnLessonDto(lesson.getId(), lesson.getTitle(), lesson.getVideoUrl(),
                lesson.getDurationSec(), lesson.getIsTrial(), lesson.getSort(),
                progress == null ? 0 : progress.getWatchedSec(),
                progress != null && Boolean.TRUE.equals(progress.getCompleted()));
    }
}
