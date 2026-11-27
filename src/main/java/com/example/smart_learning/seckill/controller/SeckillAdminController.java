package com.example.smart_learning.seckill.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.seckill.dto.SeckillActivityDto;
import com.example.smart_learning.seckill.dto.SeckillActivityRequest;
import com.example.smart_learning.seckill.dto.SeckillRecordDto;
import com.example.smart_learning.seckill.dto.SeckillStatusRequest;
import com.example.smart_learning.seckill.service.SeckillActivityService;
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
@RequestMapping("/api/v1/admin/seckill/activities")
public class SeckillAdminController {

    private final SeckillActivityService activityService;

    public SeckillAdminController(SeckillActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @RequiresPriv(module = "seckill", method = "browse")
    public ApiResponse<PageResult<SeckillActivityDto>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(activityService.page(status, page, size));
    }

    @PostMapping
    @RequiresPriv(module = "seckill", method = "create")
    public ApiResponse<SeckillActivityDto> create(@Valid @RequestBody SeckillActivityRequest request) {
        return ApiResponse.success(activityService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "seckill", method = "edit")
    public ApiResponse<SeckillActivityDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody SeckillActivityRequest request) {
        return ApiResponse.success(activityService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "seckill", method = "edit")
    public ApiResponse<SeckillActivityDto> status(@PathVariable Long id,
                                                  @Valid @RequestBody SeckillStatusRequest request) {
        return ApiResponse.success(activityService.setStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "seckill", method = "delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ApiResponse.success(null);
    }

    /** 库存对账/预热：按有效抢购记录重算 DB 库存并覆盖 Redis */
    @PostMapping("/{id}/reconcile")
    @RequiresPriv(module = "seckill", method = "edit")
    public ApiResponse<SeckillActivityDto> reconcile(@PathVariable Long id) {
        return ApiResponse.success(activityService.reconcile(id));
    }

    @GetMapping("/{id}/records")
    @RequiresPriv(module = "seckill", method = "browse")
    public ApiResponse<PageResult<SeckillRecordDto>> records(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(activityService.records(id, page, size));
    }
}
