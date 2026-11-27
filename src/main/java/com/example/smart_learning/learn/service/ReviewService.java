package com.example.smart_learning.learn.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.entity.UserCourse;
import com.example.smart_learning.learn.dto.ReviewDto;
import com.example.smart_learning.learn.dto.ReviewRequest;
import com.example.smart_learning.learn.dto.ReviewStatusRequest;
import com.example.smart_learning.learn.dto.ReviewSummaryDto;
import com.example.smart_learning.learn.entity.CourseReview;
import com.example.smart_learning.learn.repository.CourseReviewRepository;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final SysUserRepository userRepository;
    private final LearnAccessService accessService;

    public ReviewService(CourseReviewRepository reviewRepository,
                         CourseRepository courseRepository,
                         SysUserRepository userRepository,
                         LearnAccessService accessService) {
        this.reviewRepository = reviewRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    @Transactional
    public ReviewDto create(Long courseId, ReviewRequest request) {
        Long userId = accessService.requireUserId();
        UserCourse enrollment = accessService.requireEnrolled(userId, courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程不存在"));

        CourseReview review = reviewRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(CourseReview::new);
        review.setCourseId(courseId);
        review.setUserId(userId);
        if (review.getOrderId() == null) {
            review.setOrderId(enrollment.getOrderId());
        }
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        // 新建或修改后重新进入待审核
        review.setStatus("PENDING");
        review.setAdminNote(null);
        review.setReviewedBy(null);
        review.setReviewedAt(null);
        review = reviewRepository.save(review);
        recalculateRating(courseId);

        SysUser user = userRepository.findById(userId).orElse(null);
        return toDto(review, course.getTitle(), user);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryDto publicSummary(Long courseId) {
        List<CourseReview> reviews = reviewRepository.findByCourseIdAndStatusOrderByIdDesc(courseId, "APPROVED");
        Map<Long, SysUser> users = loadUsers(reviews);
        BigDecimal average = average(courseId);
        List<ReviewDto> items = reviews.stream()
                .map(review -> toDto(review, null, users.get(review.getUserId())))
                .toList();
        return new ReviewSummaryDto(courseId, average, (long) reviews.size(), items);
    }

    @Transactional(readOnly = true)
    public ReviewDto myReview(Long userId, Long courseId) {
        CourseReview review = reviewRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (review == null) {
            return null;
        }
        SysUser user = userRepository.findById(userId).orElse(null);
        String courseTitle = courseRepository.findById(courseId).map(Course::getTitle).orElse(null);
        return toDto(review, courseTitle, user);
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewDto> page(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<CourseReview> result = reviewRepository.searchAdmin(keyword, status, pageable);
        List<CourseReview> reviews = result.getContent();
        List<Long> courseIds = reviews.stream().map(CourseReview::getCourseId).distinct().toList();
        Map<Long, Course> courses = courseIds.isEmpty() ? Map.of()
                : courseRepository.findByIdIn(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, SysUser> users = loadUsers(reviews);
        List<ReviewDto> items = reviews.stream()
                .map(review -> {
                    Course course = courses.get(review.getCourseId());
                    return toDto(review, course == null ? null : course.getTitle(), users.get(review.getUserId()));
                })
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional
    public ReviewDto moderate(Long id, ReviewStatusRequest request) {
        CourseReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        review.setStatus(request.status());
        review.setAdminNote(request.adminNote() == null || request.adminNote().isBlank()
                ? null : request.adminNote().trim());
        review.setReviewedBy(accessService.requireUserId());
        review.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        review = reviewRepository.save(review);
        recalculateRating(review.getCourseId());

        String courseTitle = courseRepository.findById(review.getCourseId()).map(Course::getTitle).orElse(null);
        SysUser user = userRepository.findById(review.getUserId()).orElse(null);
        return toDto(review, courseTitle, user);
    }

    private void recalculateRating(Long courseId) {
        BigDecimal average = average(courseId);
        if (average.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        courseRepository.findById(courseId).ifPresent(course -> {
            course.setRating(average);
            courseRepository.save(course);
        });
    }

    private BigDecimal average(Long courseId) {
        List<Object[]> rows = reviewRepository.averageByCourse(courseId);
        if (rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        Object[] row = rows.get(0);
        if (row[0] == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(((Number) row[0]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, SysUser> loadUsers(List<CourseReview> reviews) {
        List<Long> userIds = reviews.stream().map(CourseReview::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private ReviewDto toDto(CourseReview review, String courseTitle, SysUser user) {
        return new ReviewDto(review.getId(), review.getCourseId(), courseTitle, review.getUserId(),
                user == null ? null : user.getAccount(),
                user == null ? null : user.getNickname(),
                review.getOrderId(), review.getRating(), review.getContent(), review.getStatus(),
                review.getAdminNote(), review.getReviewedAt(), review.getCreatedAt());
    }
}
