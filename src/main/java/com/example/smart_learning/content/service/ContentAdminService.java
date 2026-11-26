package com.example.smart_learning.content.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.content.dto.ArticleDto;
import com.example.smart_learning.content.dto.ArticleRequest;
import com.example.smart_learning.content.dto.BannerDto;
import com.example.smart_learning.content.dto.BannerRequest;
import com.example.smart_learning.content.dto.CategoryDto;
import com.example.smart_learning.content.dto.MaterialDto;
import com.example.smart_learning.content.dto.MaterialRequest;
import com.example.smart_learning.content.entity.Article;
import com.example.smart_learning.content.entity.ArticleCategory;
import com.example.smart_learning.content.entity.Banner;
import com.example.smart_learning.content.entity.Material;
import com.example.smart_learning.content.repository.ArticleCategoryRepository;
import com.example.smart_learning.content.repository.ArticleRepository;
import com.example.smart_learning.content.repository.BannerRepository;
import com.example.smart_learning.content.repository.MaterialRepository;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.permission.service.DataScopeService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Master
public class ContentAdminService {

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final BannerRepository bannerRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;

    public ContentAdminService(ArticleRepository articleRepository,
                               ArticleCategoryRepository categoryRepository,
                               MaterialRepository materialRepository,
                               BannerRepository bannerRepository,
                               DataScopeService dataScopeService,
                               AuditService auditService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
        this.bannerRepository = bannerRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleDto> articles(String keyword, String status, Long categoryId, int page, int size) {
        Long ownerId = dataScopeService.isAllScope() ? null : dataScopeService.currentUserId();
        Page<Article> result = articleRepository.search(keyword, status, categoryId, ownerId,
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)));
        Map<Long, String> categories = categoryMap();
        List<ArticleDto> items = result.getContent().stream()
                .map(article -> ArticleDto.of(article, categories.get(article.getCategoryId())))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categoryRepository.findAllByOrderBySortAscIdAsc().stream().map(CategoryDto::of).toList();
    }

    @Transactional
    public ArticleDto createArticle(ArticleRequest request) {
        Article article = new Article();
        article.setOwnerUserId(dataScopeService.currentUserId());
        apply(article, request);
        article = articleRepository.save(article);
        auditService.record("ARTICLE_CREATE", "ARTICLE", article.getId(), null, article.getTitle());
        return ArticleDto.of(article, categoryName(article.getCategoryId()));
    }

    @Transactional
    public ArticleDto updateArticle(Long id, ArticleRequest request) {
        Article article = requireArticle(id);
        dataScopeService.assertOwnerAccess(article.getOwnerUserId());
        String before = article.getTitle();
        apply(article, request);
        article = articleRepository.save(article);
        auditService.record("ARTICLE_UPDATE", "ARTICLE", id, before, article.getTitle());
        return ArticleDto.of(article, categoryName(article.getCategoryId()));
    }

    @Transactional
    public void disableArticle(Long id) {
        Article article = requireArticle(id);
        dataScopeService.assertOwnerAccess(article.getOwnerUserId());
        article.setStatus("DISABLED");
        articleRepository.save(article);
        auditService.record("ARTICLE_DISABLE", "ARTICLE", id, article.getTitle(), null);
    }

    @Transactional
    public ArticleDto publishArticle(Long id, String status) {
        if (!"PUBLISHED".equals(status) && !"DRAFT".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 PUBLISHED/DRAFT");
        }
        Article article = requireArticle(id);
        dataScopeService.assertOwnerAccess(article.getOwnerUserId());
        article.setStatus(status);
        if ("PUBLISHED".equals(status) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        article = articleRepository.save(article);
        auditService.record("ARTICLE_PUBLISH", "ARTICLE", id, status, status);
        return ArticleDto.of(article, categoryName(article.getCategoryId()));
    }

    @Transactional(readOnly = true)
    public PageResult<MaterialDto> materials(String keyword, String subject, String status, int page, int size) {
        Long ownerId = dataScopeService.isAllScope() ? null : dataScopeService.currentUserId();
        Page<Material> result = materialRepository.search(keyword, subject, status, ownerId,
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)));
        return PageResult.of(result.getContent().stream().map(MaterialDto::of).toList(),
                result.getTotalElements(), page, size);
    }

    @Transactional
    public MaterialDto createMaterial(MaterialRequest request) {
        Material material = new Material();
        material.setOwnerUserId(dataScopeService.currentUserId());
        apply(material, request);
        material = materialRepository.save(material);
        auditService.record("MATERIAL_CREATE", "MATERIAL", material.getId(), null, material.getTitle());
        return MaterialDto.of(material);
    }

    @Transactional
    public MaterialDto updateMaterial(Long id, MaterialRequest request) {
        Material material = requireMaterial(id);
        dataScopeService.assertOwnerAccess(material.getOwnerUserId());
        String before = material.getTitle();
        apply(material, request);
        material = materialRepository.save(material);
        auditService.record("MATERIAL_UPDATE", "MATERIAL", id, before, material.getTitle());
        return MaterialDto.of(material);
    }

    @Transactional
    public void disableMaterial(Long id) {
        Material material = requireMaterial(id);
        dataScopeService.assertOwnerAccess(material.getOwnerUserId());
        material.setStatus("DISABLED");
        materialRepository.save(material);
        auditService.record("MATERIAL_DISABLE", "MATERIAL", id, material.getTitle(), null);
    }

    @Transactional(readOnly = true)
    public List<BannerDto> banners() {
        return bannerRepository.findAllByOrderBySortAscIdAsc().stream().map(BannerDto::of).toList();
    }

    @Transactional
    public BannerDto createBanner(BannerRequest request) {
        Banner banner = new Banner();
        apply(banner, request);
        banner = bannerRepository.save(banner);
        auditService.record("BANNER_CREATE", "BANNER", banner.getId(), null, banner.getTitle());
        return BannerDto.of(banner);
    }

    @Transactional
    public BannerDto updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Banner 不存在"));
        apply(banner, request);
        banner = bannerRepository.save(banner);
        auditService.record("BANNER_UPDATE", "BANNER", id, null, banner.getTitle());
        return BannerDto.of(banner);
    }

    @Transactional
    public void disableBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Banner 不存在"));
        banner.setStatus("DISABLED");
        bannerRepository.save(banner);
        auditService.record("BANNER_DISABLE", "BANNER", id, banner.getTitle(), null);
    }

    private void apply(Article article, ArticleRequest request) {
        article.setCategoryId(request.categoryId());
        article.setTitle(request.title().trim());
        article.setSummary(request.summary());
        article.setCoverUrl(request.coverUrl());
        article.setContentHtml(sanitizeHtml(request.contentHtml()));
        article.setAuthor(request.author());
        article.setStatus(request.status() == null || request.status().isBlank() ? "DRAFT" : request.status());
        if ("PUBLISHED".equals(article.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
    }

    private void apply(Material material, MaterialRequest request) {
        material.setTitle(request.title().trim());
        material.setSubject(request.subject());
        material.setFileUrl(request.fileUrl());
        material.setFileSize(request.fileSize() == null ? 0L : request.fileSize());
        material.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }

    private void apply(Banner banner, BannerRequest request) {
        banner.setTitle(request.title().trim());
        banner.setImageUrl(request.imageUrl());
        banner.setLinkUrl(request.linkUrl());
        banner.setSort(request.sort() == null ? 0 : request.sort());
        banner.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
        banner.setStartAt(request.startAt());
        banner.setEndAt(request.endAt());
    }

    private String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Safelist safelist = Safelist.basicWithImages()
                .addTags("h1", "h2", "h3", "h4", "p", "br", "ul", "ol", "li", "blockquote")
                .addAttributes("a", "target", "rel")
                .addAttributes("img", "alt", "width", "height");
        return Jsoup.clean(html, safelist);
    }

    private Map<Long, String> categoryMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName));
    }

    private String categoryName(Long id) {
        return id == null ? null : categoryRepository.findById(id).map(ArticleCategory::getName).orElse(null);
    }

    private Article requireArticle(Long id) {
        return articleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
    }

    private Material requireMaterial(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料不存在"));
    }
}
