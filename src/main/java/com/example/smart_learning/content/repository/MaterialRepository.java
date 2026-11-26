package com.example.smart_learning.content.repository;

import com.example.smart_learning.content.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Query("select m from Material m where (:keyword is null or :keyword = '' " +
            "or lower(m.title) like lower(concat('%', :keyword, '%'))) " +
            "and (:subject is null or :subject = '' or m.subject = :subject) " +
            "and (:status is null or :status = '' or m.status = :status) " +
            "and (:ownerId is null or m.ownerUserId = :ownerId) " +
            "order by m.id desc")
    Page<Material> search(@Param("keyword") String keyword,
                          @Param("subject") String subject,
                          @Param("status") String status,
                          @Param("ownerId") Long ownerId,
                          Pageable pageable);

    List<Material> findByStatusOrderByIdDesc(String status);
}
