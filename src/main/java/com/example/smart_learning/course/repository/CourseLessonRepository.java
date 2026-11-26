package com.example.smart_learning.course.repository;

import com.example.smart_learning.course.entity.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {

    List<CourseLesson> findByChapterIdOrderBySortAscIdAsc(Long chapterId);

    List<CourseLesson> findByChapterIdInOrderBySortAscIdAsc(List<Long> chapterIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByChapterId(Long chapterId);
}
