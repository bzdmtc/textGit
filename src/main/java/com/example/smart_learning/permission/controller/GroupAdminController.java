package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.permission.dto.GroupDto;
import com.example.smart_learning.permission.dto.GroupPrivRequest;
import com.example.smart_learning.permission.dto.GroupRequest;
import com.example.smart_learning.permission.service.GroupService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/groups")
public class GroupAdminController {

    private final GroupService groupService;

    public GroupAdminController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @RequiresPriv(module = "group", method = "browse")
    public ApiResponse<List<GroupDto>> list() {
        return ApiResponse.success(groupService.list());
    }

    @GetMapping("/{id}/privs")
    @RequiresPriv(module = "group", method = "browse")
    public ApiResponse<List<Long>> privIds(@PathVariable Long id) {
        return ApiResponse.success(groupService.privIds(id));
    }

    @PostMapping
    @RequiresPriv(module = "group", method = "create")
    public ApiResponse<GroupDto> create(@Valid @RequestBody GroupRequest request) {
        return ApiResponse.success(groupService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "group", method = "edit")
    public ApiResponse<GroupDto> update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return ApiResponse.success(groupService.update(id, request));
    }

    @PutMapping("/{id}/privs")
    @RequiresPriv(module = "group", method = "assign-priv")
    public ApiResponse<Void> assignPrivs(@PathVariable Long id, @Valid @RequestBody GroupPrivRequest request) {
        groupService.assignPrivs(id, request.privIds());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "group", method = "delete")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        groupService.disable(id);
        return ApiResponse.success();
    }
}
