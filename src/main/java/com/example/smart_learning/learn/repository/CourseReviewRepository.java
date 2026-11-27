package com.example.smart_learning.learn.repository;

import com.example.smart_learning.learn.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseReview> findByCourseIdAndStatusOrderByIdDesc(Long courseId, String status);

    long countByCourseIdAndStatus(Long courseId, String status);

    @Query("select avg(r.rating), count(r) from CourseReview r " +
            "where r.courseId = :courseId and r.status = 'APPROVED'")
    List<Object[]> averageByCourse(@Param("courseId") Long courseId);

    @Query(value = "select r from CourseReview r, Course c, SysUser u " +
            "where r.courseId = c.id and r.userId = u.id " +
            "and (:status is null or :status = '' or r.status = :status) " +
            "and (:keyword is null or :keyword = '' " +
            "     or lower(c.title) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
            "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
            "     or lower(r.content) like lower(concat('%', :keyword, '%'))) " +
            "order by r.id desc",
            countQuery = "select count(r) from CourseReview r, Course c, SysUser u " +
                    "where r.courseId = c.id and r.userId = u.id " +
                    "and (:status is null or :status = '' or r.status = :status) " +
                    "and (:keyword is null or :keyword = '' " +
                    "     or lower(c.title) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(r.content) like lower(concat('%', :keyword, '%')))")
    Page<CourseReview> searchAdmin(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   Pageable pageable);
}
