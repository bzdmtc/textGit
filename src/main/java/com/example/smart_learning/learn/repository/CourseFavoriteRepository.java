package com.example.smart_learning.learn.repository;

import com.example.smart_learning.learn.entity.CourseFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseFavoriteRepository extends JpaRepository<CourseFavorite, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseFavorite> findByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseFavorite> findByUserIdOrderByIdDesc(Long userId);
}
