package com.example.smart_learning.learn.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.entity.UserCourse;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.learn.dto.FavoriteDto;
import com.example.smart_learning.learn.dto.FavoriteStatusDto;
import com.example.smart_learning.learn.entity.CourseFavorite;
import com.example.smart_learning.learn.repository.CourseFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class FavoriteService {

    private final CourseFavoriteRepository favoriteRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final LearnAccessService accessService;

    public FavoriteService(CourseFavoriteRepository favoriteRepository,
                           CourseRepository courseRepository,
                           UserCourseRepository userCourseRepository,
                           LearnAccessService accessService) {
        this.favoriteRepository = favoriteRepository;
        this.courseRepository = courseRepository;
        this.userCourseRepository = userCourseRepository;
        this.accessService = accessService;
    }

    @Transactional
    public FavoriteStatusDto add(Long courseId) {
        Long userId = accessService.requireUserId();
        Course course = courseRepository.findById(courseId)
                .filter(item -> "PUBLISHED".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程不存在或未上架"));
        if (!favoriteRepository.existsByUserIdAndCourseId(userId, courseId)) {
            CourseFavorite favorite = new CourseFavorite();
            favorite.setUserId(userId);
            favorite.setCourseId(course.getId());
            favoriteRepository.save(favorite);
        }
        return new FavoriteStatusDto(courseId, true);
    }

    @Transactional
    public FavoriteStatusDto remove(Long courseId) {
        Long userId = accessService.requireUserId();
        favoriteRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(favoriteRepository::delete);
        return new FavoriteStatusDto(courseId, false);
    }

    @Transactional(readOnly = true)
    public FavoriteStatusDto status(Long courseId) {
        Long userId = accessService.requireUserId();
        return new FavoriteStatusDto(courseId,
                favoriteRepository.existsByUserIdAndCourseId(userId, courseId));
    }

    @Transactional(readOnly = true)
    public List<FavoriteDto> list() {
        Long userId = accessService.requireUserId();
        List<CourseFavorite> favorites = favoriteRepository.findByUserIdOrderByIdDesc(userId);
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = favorites.stream().map(CourseFavorite::getCourseId).distinct().toList();
        Map<Long, Course> courses = courseRepository.findByIdIn(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, UserCourse> enrollments = userCourseRepository
                .findByUserIdAndCourseIdIn(userId, courseIds).stream()
                .collect(Collectors.toMap(UserCourse::getCourseId, Function.identity()));
        return favorites.stream()
                .map(favorite -> {
                    Course course = courses.get(favorite.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    UserCourse enrollment = enrollments.get(course.getId());
                    BigDecimal progress = enrollment == null ? BigDecimal.ZERO : enrollment.getProgressPercent();
                    return new FavoriteDto(favorite.getId(), course.getId(), course.getTitle(),
                            course.getCoverUrl(), course.getSubject(), course.getStage(), course.getPrice(),
                            course.getRating(), course.getStudentCount(), enrollment != null, progress,
                            favorite.getCreatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
