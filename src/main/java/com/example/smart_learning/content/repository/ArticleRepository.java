package com.example.smart_learning.content.repository;

import com.example.smart_learning.content.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("select a from Article a where (:keyword is null or :keyword = '' " +
            "or lower(a.title) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or :status = '' or a.status = :status) " +
            "and (:categoryId is null or a.categoryId = :categoryId) " +
            "and (:ownerId is null or a.ownerUserId = :ownerId) " +
            "order by a.id desc")
    Page<Article> search(@Param("keyword") String keyword,
                         @Param("status") String status,
                         @Param("categoryId") Long categoryId,
                         @Param("ownerId") Long ownerId,
                         Pageable pageable);

    List<Article> findTop6ByStatusOrderByPublishedAtDesc(String status);

    Optional<Article> findByIdAndStatus(Long id, String status);
}
