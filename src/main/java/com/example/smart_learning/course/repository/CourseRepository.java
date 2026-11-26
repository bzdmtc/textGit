package com.example.smart_learning.course.repository;

import com.example.smart_learning.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("select c from Course c where (:keyword is null or :keyword = '' " +
            "or lower(c.title) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(c.subtitle, '')) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or :status = '' or c.status = :status) " +
            "and (:subject is null or :subject = '' or c.subject = :subject) " +
            "and (:stage is null or :stage = '' or c.stage = :stage) " +
            "and (:ownerId is null or c.ownerUserId = :ownerId) " +
            "order by c.id desc")
    Page<Course> search(@Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("subject") String subject,
                        @Param("stage") String stage,
                        @Param("ownerId") Long ownerId,
                        Pageable pageable);

    List<Course> findTop8ByStatusOrderByIdDesc(String status);

    List<Course> findByStatusOrderByIdDesc(String status);

    List<Course> findByIdIn(List<Long> ids);

    Optional<Course> findByIdAndStatus(Long id, String status);
}
