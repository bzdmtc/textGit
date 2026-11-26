package com.example.smart_learning.course.repository;

import com.example.smart_learning.course.entity.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {

    List<CourseTeacher> findByCourseIdOrderBySortAscIdAsc(Long courseId);

    List<CourseTeacher> findByCourseIdIn(List<Long> courseIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByCourseId(Long courseId);
}
