package com.example.smart_learning.course.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.dto.TeacherRequest;
import com.example.smart_learning.course.service.TeacherAdminService;
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
@RequestMapping("/api/v1/admin/teachers")
public class TeacherAdminController {

    private final TeacherAdminService teacherAdminService;

    public TeacherAdminController(TeacherAdminService teacherAdminService) {
        this.teacherAdminService = teacherAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "teacher", method = "browse")
    public ApiResponse<List<TeacherDto>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(teacherAdminService.list(keyword));
    }

    @PostMapping
    @RequiresPriv(module = "teacher", method = "create")
    public ApiResponse<TeacherDto> create(@Valid @RequestBody TeacherRequest request) {
        return ApiResponse.success(teacherAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "teacher", method = "edit")
    public ApiResponse<TeacherDto> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ApiResponse.success(teacherAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "teacher", method = "delete")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        teacherAdminService.disable(id);
        return ApiResponse.success();
    }
}
