package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.permission.dto.PositionDto;
import com.example.smart_learning.permission.dto.PositionRequest;
import com.example.smart_learning.permission.dto.PositionStatusRequest;
import com.example.smart_learning.permission.service.PositionService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/positions")
public class PositionAdminController {

    private final PositionService positionService;

    public PositionAdminController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @RequiresPriv(module = "user", method = "browse")
    public ApiResponse<List<PositionDto>> list() {
        return ApiResponse.success(positionService.list());
    }

    @PostMapping
    @RequiresPriv(module = "user", method = "edit")
    public ApiResponse<PositionDto> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(positionService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "user", method = "edit")
    public ApiResponse<PositionDto> update(@PathVariable Long id,
                                           @Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(positionService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "user", method = "edit")
    public ApiResponse<PositionDto> status(@PathVariable Long id,
                                           @Valid @RequestBody PositionStatusRequest request) {
        return ApiResponse.success(positionService.changeStatus(id, request.status()));
    }
}
