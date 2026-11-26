package com.example.smart_learning.enrollment.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.dto.MyCourseDto;
import com.example.smart_learning.enrollment.entity.UserCourse;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.entity.OrderItem;
import com.example.smart_learning.order.repository.OrderItemRepository;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class EnrollmentService {

    private final UserCourseRepository userCourseRepository;
    private final OrderItemRepository orderItemRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(UserCourseRepository userCourseRepository,
                             OrderItemRepository orderItemRepository,
                             CourseRepository courseRepository) {
        this.userCourseRepository = userCourseRepository;
        this.orderItemRepository = orderItemRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 支付成功后开通选课，幂等：已存在选课关系时直接跳过。
     */
    public void enroll(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (OrderItem item : items) {
            if (userCourseRepository.existsByUserIdAndCourseId(order.getUserId(), item.getCourseId())) {
                continue;
            }
            UserCourse userCourse = new UserCourse();
            userCourse.setUserId(order.getUserId());
            userCourse.setCourseId(item.getCourseId());
            userCourse.setOrderId(order.getId());
            userCourse.setEnrolledAt(now);
            userCourse.setProgressPercent(BigDecimal.ZERO);
            userCourse.setStatus("ACTIVE");
            userCourseRepository.save(userCourse);

            courseRepository.findById(item.getCourseId()).ifPresent(course -> {
                course.setStudentCount((course.getStudentCount() == null ? 0 : course.getStudentCount()) + 1);
                courseRepository.save(course);
            });
        }
    }

    /**
     * 退款通过后撤销该订单开通的课程。
     */
    public void revoke(Order order) {
        List<UserCourse> enrollments = userCourseRepository.findByOrderId(order.getId());
        for (UserCourse enrollment : enrollments) {
            userCourseRepository.delete(enrollment);
            courseRepository.findById(enrollment.getCourseId()).ifPresent(course -> {
                int students = course.getStudentCount() == null ? 0 : course.getStudentCount();
                course.setStudentCount(Math.max(0, students - 1));
                courseRepository.save(course);
            });
        }
    }

    /**
     * 我的课程要求强一致：支付/退款后立即反映，固定走主库。
     */
    @Transactional(readOnly = true)
    public List<MyCourseDto> myCourses() {
        Long userId = SecurityUtils.currentUserId();
        List<UserCourse> enrollments = userCourseRepository.findByUserIdOrderByIdDesc(userId);
        if (enrollments.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = enrollments.stream().map(UserCourse::getCourseId).distinct().toList();
        Map<Long, Course> courseMap = courseRepository.findByIdIn(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return enrollments.stream()
                .map(enrollment -> {
                    Course course = courseMap.get(enrollment.getCourseId());
                    return course == null ? null : MyCourseDto.of(enrollment, course);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
