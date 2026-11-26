package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.permission.dto.PrivDto;
import com.example.smart_learning.permission.service.PrivService;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/privs")
public class PrivAdminController {

    private final PrivService privService;

    public PrivAdminController(PrivService privService) {
        this.privService = privService;
    }

    @GetMapping
    @RequiresPriv(module = "priv", method = "browse")
    public ApiResponse<List<PrivDto>> list() {
        return ApiResponse.success(privService.list());
    }

    @GetMapping("/tree")
    @RequiresPriv(module = "priv", method = "browse")
    public ApiResponse<List<PrivDto>> tree() {
        return ApiResponse.success(privService.list());
    }
}
