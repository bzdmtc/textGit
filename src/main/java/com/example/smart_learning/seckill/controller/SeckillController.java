package com.example.smart_learning.seckill.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.seckill.dto.PublicSeckillDto;
import com.example.smart_learning.seckill.dto.SeckillClaimResult;
import com.example.smart_learning.seckill.dto.SeckillResultDto;
import com.example.smart_learning.seckill.service.SeckillActivityService;
import com.example.smart_learning.seckill.service.SeckillPurchaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seckill")
public class SeckillController {

    private final SeckillActivityService activityService;
    private final SeckillPurchaseService purchaseService;

    public SeckillController(SeckillActivityService activityService,
                             SeckillPurchaseService purchaseService) {
        this.activityService = activityService;
        this.purchaseService = purchaseService;
    }

    @GetMapping("/activities")
    public ApiResponse<List<PublicSeckillDto>> activities() {
        return ApiResponse.success(activityService.visible());
    }

    @PostMapping("/activities/{id}/claim")
    public ApiResponse<SeckillClaimResult> claim(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.claim(id));
    }

    @GetMapping("/activities/{id}/result")
    public ApiResponse<SeckillResultDto> result(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.result(id));
    }
}
