package com.example.smart_learning.course.repository;

import com.example.smart_learning.course.entity.CourseChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, Long> {

    List<CourseChapter> findByCourseIdOrderBySortAscIdAsc(Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByCourseId(Long courseId);
}
