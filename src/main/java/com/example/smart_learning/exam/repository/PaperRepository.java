package com.example.smart_learning.exam.repository;

import com.example.smart_learning.exam.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    @Query("select p from Paper p where " +
            "(:keyword is null or :keyword = '' " +
            "or lower(p.title) like lower(concat('%', :keyword, '%'))) " +
            "and (:type is null or :type = '' or p.type = :type) " +
            "and (:subject is null or :subject = '' or p.subject = :subject) " +
            "and (:status is null or :status = '' or p.status = :status) " +
            "and (:ownerId is null or p.ownerUserId = :ownerId) " +
            "order by p.id desc")
    Page<Paper> search(@Param("keyword") String keyword,
                       @Param("type") String type,
                       @Param("subject") String subject,
                       @Param("status") String status,
                       @Param("ownerId") Long ownerId,
                       Pageable pageable);

    List<Paper> findByStatusOrderByIdDesc(String status);
}
