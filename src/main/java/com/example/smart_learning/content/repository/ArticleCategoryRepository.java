package com.example.smart_learning.content.repository;

import com.example.smart_learning.content.entity.ArticleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {

    List<ArticleCategory> findAllByOrderBySortAscIdAsc();
}
