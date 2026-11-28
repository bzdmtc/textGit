package com.example.smart_learning.university.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.university.dto.UniversityDetailDto;
import com.example.smart_learning.university.dto.UniversityDto;
import com.example.smart_learning.university.dto.UniversityOptionsDto;
import com.example.smart_learning.university.service.UniversityPublicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/universities")
public class UniversityPublicController {

    private final UniversityPublicService universityPublicService;

    public UniversityPublicController(UniversityPublicService universityPublicService) {
        this.universityPublicService = universityPublicService;
    }

    @GetMapping
    public ApiResponse<PageResult<UniversityDto>> page(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String province,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(universityPublicService.page(keyword, province, level, type, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<UniversityOptionsDto> options() {
        return ApiResponse.success(universityPublicService.options());
    }

    @GetMapping("/featured")
    public ApiResponse<List<UniversityDto>> featured(@RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(universityPublicService.featured(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<UniversityDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.success(universityPublicService.detail(id));
    }
}
