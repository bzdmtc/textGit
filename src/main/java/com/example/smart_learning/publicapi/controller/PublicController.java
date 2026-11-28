package com.example.smart_learning.publicapi.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.content.dto.ArticleDto;
import com.example.smart_learning.content.dto.BannerDto;
import com.example.smart_learning.content.dto.CategoryDto;
import com.example.smart_learning.content.dto.HomeDto;
import com.example.smart_learning.content.dto.MaterialDto;
import com.example.smart_learning.content.service.ContentPublicService;
import com.example.smart_learning.coupon.dto.CouponDto;
import com.example.smart_learning.coupon.service.CouponUserService;
import com.example.smart_learning.course.dto.CourseDetailDto;
import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.service.CoursePublicService;
import com.example.smart_learning.course.service.TeacherPublicService;
import com.example.smart_learning.university.service.UniversityPublicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final CoursePublicService coursePublicService;
    private final TeacherPublicService teacherPublicService;
    private final ContentPublicService contentPublicService;
    private final CouponUserService couponUserService;
    private final UniversityPublicService universityPublicService;

    public PublicController(CoursePublicService coursePublicService,
                            TeacherPublicService teacherPublicService,
                            ContentPublicService contentPublicService,
                            CouponUserService couponUserService,
                            UniversityPublicService universityPublicService) {
        this.coursePublicService = coursePublicService;
        this.teacherPublicService = teacherPublicService;
        this.contentPublicService = contentPublicService;
        this.couponUserService = couponUserService;
        this.universityPublicService = universityPublicService;
    }

    @GetMapping("/home")
    public ApiResponse<HomeDto> home() {
        return ApiResponse.success(new HomeDto(
                contentPublicService.banners(),
                coursePublicService.hot(),
                teacherPublicService.list(),
                contentPublicService.latestArticles(),
                universityPublicService.featured(6)));
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<CourseDto>> courses(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String subject,
                                                      @RequestParam(required = false) String stage,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(coursePublicService.page(keyword, subject, stage, page, size));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<CourseDetailDto> course(@PathVariable Long id) {
        return ApiResponse.success(coursePublicService.detail(id));
    }

    @GetMapping("/teachers")
    public ApiResponse<List<TeacherDto>> teachers() {
        return ApiResponse.success(teacherPublicService.list());
    }

    @GetMapping("/teachers/{id}")
    public ApiResponse<TeacherDto> teacher(@PathVariable Long id) {
        TeacherDto teacher = teacherPublicService.detail(id);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "讲师不存在");
        }
        return ApiResponse.success(teacher);
    }

    @GetMapping("/article-categories")
    public ApiResponse<List<CategoryDto>> categories() {
        return ApiResponse.success(contentPublicService.categories());
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<ArticleDto>> articles(@RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(contentPublicService.articles(categoryId, keyword, page, size));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleDto> article(@PathVariable Long id) {
        return ApiResponse.success(contentPublicService.articleDetail(id));
    }

    @GetMapping("/materials")
    public ApiResponse<List<MaterialDto>> materials(@RequestParam(required = false) String subject) {
        return ApiResponse.success(contentPublicService.materials(subject));
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerDto>> banners() {
        return ApiResponse.success(contentPublicService.banners());
    }

    @GetMapping("/coupons")
    public ApiResponse<List<CouponDto>> coupons(@RequestParam(required = false) Long courseId,
                                                @RequestParam(required = false) String subject) {
        if (courseId != null) {
            return ApiResponse.success(couponUserService.availableForCourse(courseId));
        }
        return ApiResponse.success(couponUserService.availableForSubject(subject));
    }
}
