package com.example.smart_learning.university.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.security.RequiresPriv;
import com.example.smart_learning.university.dto.AdmissionStatDto;
import com.example.smart_learning.university.dto.AdmissionStatRequest;
import com.example.smart_learning.university.dto.MajorDto;
import com.example.smart_learning.university.dto.MajorRequest;
import com.example.smart_learning.university.dto.ScoreLineDto;
import com.example.smart_learning.university.dto.ScoreLineRequest;
import com.example.smart_learning.university.dto.UniversityDetailDto;
import com.example.smart_learning.university.dto.UniversityDto;
import com.example.smart_learning.university.dto.UniversityOptionsDto;
import com.example.smart_learning.university.dto.UniversityRequest;
import com.example.smart_learning.university.dto.UniversityStatusRequest;
import com.example.smart_learning.university.service.UniversityAdminService;
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
@RequestMapping("/api/v1/admin/universities")
public class UniversityAdminController {

    private final UniversityAdminService universityAdminService;

    public UniversityAdminController(UniversityAdminService universityAdminService) {
        this.universityAdminService = universityAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "university", method = "browse")
    public ApiResponse<PageResult<UniversityDto>> page(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String province,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(universityAdminService.page(keyword, province, level, type, status, page, size));
    }

    @GetMapping("/options")
    @RequiresPriv(module = "university", method = "browse")
    public ApiResponse<UniversityOptionsDto> options() {
        return ApiResponse.success(universityAdminService.options());
    }

    @GetMapping("/{id}")
    @RequiresPriv(module = "university", method = "browse")
    public ApiResponse<UniversityDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.success(universityAdminService.detail(id));
    }

    @PostMapping
    @RequiresPriv(module = "university", method = "create")
    public ApiResponse<UniversityDto> create(@Valid @RequestBody UniversityRequest request) {
        return ApiResponse.success(universityAdminService.createUniversity(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "university", method = "edit")
    public ApiResponse<UniversityDto> update(@PathVariable Long id,
                                             @Valid @RequestBody UniversityRequest request) {
        return ApiResponse.success(universityAdminService.updateUniversity(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "university", method = "edit")
    public ApiResponse<UniversityDto> status(@PathVariable Long id,
                                             @Valid @RequestBody UniversityStatusRequest request) {
        return ApiResponse.success(universityAdminService.setUniversityStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "university", method = "delete")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        universityAdminService.setUniversityStatus(id, "DISABLED");
        return ApiResponse.success();
    }

    @PostMapping("/{id}/majors")
    @RequiresPriv(module = "university", method = "create")
    public ApiResponse<MajorDto> createMajor(@PathVariable Long id,
                                             @Valid @RequestBody MajorRequest request) {
        return ApiResponse.success(universityAdminService.createMajor(id, request));
    }

    @PutMapping("/{id}/majors/{majorId}")
    @RequiresPriv(module = "university", method = "edit")
    public ApiResponse<MajorDto> updateMajor(@PathVariable Long id, @PathVariable Long majorId,
                                             @Valid @RequestBody MajorRequest request) {
        return ApiResponse.success(universityAdminService.updateMajor(id, majorId, request));
    }

    @DeleteMapping("/{id}/majors/{majorId}")
    @RequiresPriv(module = "university", method = "delete")
    public ApiResponse<Void> disableMajor(@PathVariable Long id, @PathVariable Long majorId) {
        universityAdminService.disableMajor(id, majorId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/score-lines")
    @RequiresPriv(module = "university", method = "create")
    public ApiResponse<ScoreLineDto> createScoreLine(@PathVariable Long id,
                                                     @Valid @RequestBody ScoreLineRequest request) {
        return ApiResponse.success(universityAdminService.createScoreLine(id, request));
    }

    @PutMapping("/{id}/score-lines/{lineId}")
    @RequiresPriv(module = "university", method = "edit")
    public ApiResponse<ScoreLineDto> updateScoreLine(@PathVariable Long id, @PathVariable Long lineId,
                                                     @Valid @RequestBody ScoreLineRequest request) {
        return ApiResponse.success(universityAdminService.updateScoreLine(id, lineId, request));
    }

    @DeleteMapping("/{id}/score-lines/{lineId}")
    @RequiresPriv(module = "university", method = "delete")
    public ApiResponse<Void> deleteScoreLine(@PathVariable Long id, @PathVariable Long lineId) {
        universityAdminService.deleteScoreLine(id, lineId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/admission-stats")
    @RequiresPriv(module = "university", method = "create")
    public ApiResponse<AdmissionStatDto> createAdmissionStat(@PathVariable Long id,
                                                             @Valid @RequestBody AdmissionStatRequest request) {
        return ApiResponse.success(universityAdminService.createAdmissionStat(id, request));
    }

    @PutMapping("/{id}/admission-stats/{statId}")
    @RequiresPriv(module = "university", method = "edit")
    public ApiResponse<AdmissionStatDto> updateAdmissionStat(@PathVariable Long id, @PathVariable Long statId,
                                                             @Valid @RequestBody AdmissionStatRequest request) {
        return ApiResponse.success(universityAdminService.updateAdmissionStat(id, statId, request));
    }

    @DeleteMapping("/{id}/admission-stats/{statId}")
    @RequiresPriv(module = "university", method = "delete")
    public ApiResponse<Void> deleteAdmissionStat(@PathVariable Long id, @PathVariable Long statId) {
        universityAdminService.deleteAdmissionStat(id, statId);
        return ApiResponse.success();
    }
}
