package com.example.smart_learning.stats.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.security.RequiresPriv;
import com.example.smart_learning.stats.dto.DashboardDto;
import com.example.smart_learning.stats.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    @RequiresPriv(module = "admin", method = "access")
    public ApiResponse<DashboardDto> dashboard(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.success(statsService.dashboard(days));
    }
}
