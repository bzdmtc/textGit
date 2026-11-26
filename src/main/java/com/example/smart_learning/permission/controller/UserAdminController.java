package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.permission.dto.UserCreateRequest;
import com.example.smart_learning.permission.dto.UserDto;
import com.example.smart_learning.permission.dto.UserGroupRequest;
import com.example.smart_learning.permission.dto.UserPermissionView;
import com.example.smart_learning.permission.dto.UserStatusRequest;
import com.example.smart_learning.permission.dto.UserUpdateRequest;
import com.example.smart_learning.permission.service.UserAdminService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "user", method = "browse")
    public ApiResponse<PageResult<UserDto>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userAdminService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPriv(module = "user", method = "browse")
    public ApiResponse<UserDto> get(@PathVariable Long id) {
        return ApiResponse.success(userAdminService.get(id));
    }

    @PostMapping
    @RequiresPriv(module = "user", method = "create")
    public ApiResponse<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "user", method = "edit")
    public ApiResponse<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "user", method = "disable")
    public ApiResponse<UserDto> status(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.success(userAdminService.setStatus(id, request.status()));
    }

    @PutMapping("/{id}/groups")
    @RequiresPriv(module = "user", method = "assign-group")
    public ApiResponse<UserDto> assignGroups(@PathVariable Long id, @Valid @RequestBody UserGroupRequest request) {
        return ApiResponse.success(userAdminService.assignGroups(id, request.groupIds()));
    }

    @GetMapping("/{id}/permissions")
    @RequiresPriv(module = "user", method = "browse")
    public ApiResponse<UserPermissionView> permissions(@PathVariable Long id) {
        return ApiResponse.success(userAdminService.permissions(id));
    }
}
