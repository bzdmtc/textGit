package com.example.smart_learning.content.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.datasource.ReadOnly;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.content.dto.ArticleDto;
import com.example.smart_learning.content.dto.BannerDto;
import com.example.smart_learning.content.dto.CategoryDto;
import com.example.smart_learning.content.dto.MaterialDto;
import com.example.smart_learning.content.entity.Article;
import com.example.smart_learning.content.entity.ArticleCategory;
import com.example.smart_learning.content.repository.ArticleCategoryRepository;
import com.example.smart_learning.content.repository.ArticleRepository;
import com.example.smart_learning.content.repository.BannerRepository;
import com.example.smart_learning.content.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentPublicService {

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final BannerRepository bannerRepository;

    public ContentPublicService(ArticleRepository articleRepository,
                                ArticleCategoryRepository categoryRepository,
                                MaterialRepository materialRepository,
                                BannerRepository bannerRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
        this.bannerRepository = bannerRepository;
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categoryRepository.findAllByOrderBySortAscIdAsc().stream().map(CategoryDto::of).toList();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public PageResult<ArticleDto> articles(Long categoryId, String keyword, int page, int size) {
        Page<Article> result = articleRepository.search(keyword, "PUBLISHED", categoryId, null,
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 50)));
        Map<Long, String> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName));
        return PageResult.of(result.getContent().stream()
                        .map(article -> ArticleDto.of(article, categories.get(article.getCategoryId())))
                        .toList(),
                result.getTotalElements(), page, size);
    }

    @Master
    @Transactional
    public ArticleDto articleDetail(Long id) {
        Article article = articleRepository.findByIdAndStatus(id, "PUBLISHED")
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        article = articleRepository.save(article);
        String categoryName = article.getCategoryId() == null ? null
                : categoryRepository.findById(article.getCategoryId()).map(ArticleCategory::getName).orElse(null);
        return ArticleDto.of(article, categoryName);
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<ArticleDto> latestArticles() {
        Map<Long, String> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName));
        return articleRepository.findTop6ByStatusOrderByPublishedAtDesc("PUBLISHED").stream()
                .map(article -> ArticleDto.of(article, categories.get(article.getCategoryId())))
                .toList();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<MaterialDto> materials(String subject) {
        return materialRepository.search(null, subject, "ACTIVE", null,
                        PageRequest.of(0, 100)).getContent().stream()
                .map(MaterialDto::of)
                .toList();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<BannerDto> banners() {
        return bannerRepository.findByStatusOrderBySortAscIdAsc("ACTIVE").stream()
                .map(BannerDto::of)
                .toList();
    }
}
