package com.example.smart_learning.enrollment.repository;

import com.example.smart_learning.enrollment.entity.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);

    List<UserCourse> findByUserIdOrderByIdDesc(Long userId);

    List<UserCourse> findByOrderId(Long orderId);

    List<UserCourse> findByUserIdAndCourseIdIn(Long userId, List<Long> courseIds);
}
