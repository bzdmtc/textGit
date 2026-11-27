package com.example.smart_learning.learn.repository;

import com.example.smart_learning.learn.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(Long userId, List<Long> lessonIds);

    Optional<LessonProgress> findTopByUserIdAndLessonIdInOrderByUpdatedAtDesc(Long userId, List<Long> lessonIds);

    long countByUserIdAndLessonIdInAndCompletedTrue(Long userId, List<Long> lessonIds);
}
