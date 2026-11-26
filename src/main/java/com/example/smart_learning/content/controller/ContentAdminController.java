package com.example.smart_learning.content.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.content.dto.ArticleDto;
import com.example.smart_learning.content.dto.ArticleRequest;
import com.example.smart_learning.content.dto.BannerDto;
import com.example.smart_learning.content.dto.BannerRequest;
import com.example.smart_learning.content.dto.CategoryDto;
import com.example.smart_learning.content.dto.MaterialDto;
import com.example.smart_learning.content.dto.MaterialRequest;
import com.example.smart_learning.content.dto.PublishStatusRequest;
import com.example.smart_learning.content.service.ContentAdminService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class ContentAdminController {

    private final ContentAdminService contentAdminService;

    public ContentAdminController(ContentAdminService contentAdminService) {
        this.contentAdminService = contentAdminService;
    }

    @GetMapping("/articles")
    @RequiresPriv(module = "article", method = "browse")
    public ApiResponse<PageResult<ArticleDto>> articles(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(contentAdminService.articles(keyword, status, categoryId, page, size));
    }

    @GetMapping("/article-categories")
    @RequiresPriv(module = "article", method = "browse")
    public ApiResponse<List<CategoryDto>> categories() {
        return ApiResponse.success(contentAdminService.categories());
    }

    @PostMapping("/articles")
    @RequiresPriv(module = "article", method = "create")
    public ApiResponse<ArticleDto> createArticle(@Valid @RequestBody ArticleRequest request) {
        return ApiResponse.success(contentAdminService.createArticle(request));
    }

    @PutMapping("/articles/{id}")
    @RequiresPriv(module = "article", method = "edit")
    public ApiResponse<ArticleDto> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return ApiResponse.success(contentAdminService.updateArticle(id, request));
    }

    @PutMapping("/articles/{id}/status")
    @RequiresPriv(module = "article", method = "publish")
    public ApiResponse<ArticleDto> publishArticle(@PathVariable Long id,
                                                  @Valid @RequestBody PublishStatusRequest request) {
        return ApiResponse.success(contentAdminService.publishArticle(id, request.status()));
    }

    @DeleteMapping("/articles/{id}")
    @RequiresPriv(module = "article", method = "delete")
    public ApiResponse<Void> disableArticle(@PathVariable Long id) {
        contentAdminService.disableArticle(id);
        return ApiResponse.success();
    }

    @GetMapping("/materials")
    @RequiresPriv(module = "material", method = "browse")
    public ApiResponse<PageResult<MaterialDto>> materials(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String subject,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(contentAdminService.materials(keyword, subject, status, page, size));
    }

    @PostMapping("/materials")
    @RequiresPriv(module = "material", method = "create")
    public ApiResponse<MaterialDto> createMaterial(@Valid @RequestBody MaterialRequest request) {
        return ApiResponse.success(contentAdminService.createMaterial(request));
    }

    @PutMapping("/materials/{id}")
    @RequiresPriv(module = "material", method = "edit")
    public ApiResponse<MaterialDto> updateMaterial(@PathVariable Long id,
                                                   @Valid @RequestBody MaterialRequest request) {
        return ApiResponse.success(contentAdminService.updateMaterial(id, request));
    }

    @DeleteMapping("/materials/{id}")
    @RequiresPriv(module = "material", method = "delete")
    public ApiResponse<Void> disableMaterial(@PathVariable Long id) {
        contentAdminService.disableMaterial(id);
        return ApiResponse.success();
    }

    @GetMapping("/banners")
    @RequiresPriv(module = "banner", method = "browse")
    public ApiResponse<List<BannerDto>> banners() {
        return ApiResponse.success(contentAdminService.banners());
    }

    @PostMapping("/banners")
    @RequiresPriv(module = "banner", method = "create")
    public ApiResponse<BannerDto> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.success(contentAdminService.createBanner(request));
    }

    @PutMapping("/banners/{id}")
    @RequiresPriv(module = "banner", method = "edit")
    public ApiResponse<BannerDto> updateBanner(@PathVariable Long id,
                                               @Valid @RequestBody BannerRequest request) {
        return ApiResponse.success(contentAdminService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    @RequiresPriv(module = "banner", method = "delete")
    public ApiResponse<Void> disableBanner(@PathVariable Long id) {
        contentAdminService.disableBanner(id);
        return ApiResponse.success();
    }
}
