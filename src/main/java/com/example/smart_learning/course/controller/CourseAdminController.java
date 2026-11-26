package com.example.smart_learning.course.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.course.dto.ChapterDto;
import com.example.smart_learning.course.dto.ChapterRequest;
import com.example.smart_learning.course.dto.CourseDetailDto;
import com.example.smart_learning.course.dto.CourseDto;
import com.example.smart_learning.course.dto.CourseRequest;
import com.example.smart_learning.course.dto.CourseStatusRequest;
import com.example.smart_learning.course.dto.LessonDto;
import com.example.smart_learning.course.dto.LessonRequest;
import com.example.smart_learning.course.service.CourseAdminService;
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

@RestController
@RequestMapping("/api/v1/admin/courses")
public class CourseAdminController {

    private final CourseAdminService courseAdminService;

    public CourseAdminController(CourseAdminService courseAdminService) {
        this.courseAdminService = courseAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "course", method = "browse")
    public ApiResponse<PageResult<CourseDto>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String subject,
                                                   @RequestParam(required = false) String stage,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(courseAdminService.page(keyword, status, subject, stage, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPriv(module = "course", method = "browse")
    public ApiResponse<CourseDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.success(courseAdminService.detail(id));
    }

    @PostMapping
    @RequiresPriv(module = "course", method = "create")
    public ApiResponse<CourseDto> create(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<CourseDto> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "course", method = "publish")
    public ApiResponse<CourseDto> status(@PathVariable Long id, @Valid @RequestBody CourseStatusRequest request) {
        return ApiResponse.success(courseAdminService.publish(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "course", method = "delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseAdminService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{courseId}/chapters")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<ChapterDto> addChapter(@PathVariable Long courseId,
                                              @Valid @RequestBody ChapterRequest request) {
        return ApiResponse.success(courseAdminService.addChapter(courseId, request));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<ChapterDto> updateChapter(@PathVariable Long courseId,
                                                 @PathVariable Long chapterId,
                                                 @Valid @RequestBody ChapterRequest request) {
        return ApiResponse.success(courseAdminService.updateChapter(chapterId, request));
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<Void> deleteChapter(@PathVariable Long courseId, @PathVariable Long chapterId) {
        courseAdminService.deleteChapter(chapterId);
        return ApiResponse.success();
    }

    @PostMapping("/{courseId}/chapters/{chapterId}/lessons")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<LessonDto> addLesson(@PathVariable Long courseId,
                                            @PathVariable Long chapterId,
                                            @Valid @RequestBody LessonRequest request) {
        return ApiResponse.success(courseAdminService.addLesson(chapterId, request));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<LessonDto> updateLesson(@PathVariable Long courseId,
                                               @PathVariable Long chapterId,
                                               @PathVariable Long lessonId,
                                               @Valid @RequestBody LessonRequest request) {
        return ApiResponse.success(courseAdminService.updateLesson(lessonId, request));
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    @RequiresPriv(module = "course", method = "edit")
    public ApiResponse<Void> deleteLesson(@PathVariable Long courseId,
                                          @PathVariable Long chapterId,
                                          @PathVariable Long lessonId) {
        courseAdminService.deleteLesson(lessonId);
        return ApiResponse.success();
    }
}
