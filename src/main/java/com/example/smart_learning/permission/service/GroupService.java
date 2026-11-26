package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.permission.dto.GroupDto;
import com.example.smart_learning.permission.dto.GroupRequest;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysGroupPriv;
import com.example.smart_learning.permission.entity.SysPriv;
import com.example.smart_learning.permission.repository.SysGroupPrivRepository;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysPrivRepository;
import com.example.smart_learning.permission.repository.SysUserGroupRepository;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Master
public class GroupService {

    private final SysGroupRepository groupRepository;
    private final SysUserGroupRepository userGroupRepository;
    private final SysGroupPrivRepository groupPrivRepository;
    private final SysPrivRepository privRepository;
    private final SysUserRepository userRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public GroupService(SysGroupRepository groupRepository,
                        SysUserGroupRepository userGroupRepository,
                        SysGroupPrivRepository groupPrivRepository,
                        SysPrivRepository privRepository,
                        SysUserRepository userRepository,
                        PermissionService permissionService,
                        AuditService auditService) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupPrivRepository = groupPrivRepository;
        this.privRepository = privRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<GroupDto> list() {
        return groupRepository.findAllByOrderBySortAscIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> privIds(Long groupId) {
        requireGroup(groupId);
        return groupPrivRepository.findPrivIdsByGroupId(groupId);
    }

    @Transactional
    public GroupDto create(GroupRequest request) {
        String code = request.code().trim().toUpperCase();
        if (groupRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.GROUP_CODE_EXISTS);
        }
        SysGroup group = new SysGroup();
        apply(group, request, code);
        group = groupRepository.save(group);
        auditService.record("GROUP_CREATE", "GROUP", group.getId(), null, toDto(group));
        return toDto(group);
    }

    @Transactional
    public GroupDto update(Long id, GroupRequest request) {
        SysGroup group = requireGroup(id);
        GroupDto before = toDto(group);
        String code = request.code().trim().toUpperCase();
        if (!code.equals(group.getCode()) && groupRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.GROUP_CODE_EXISTS);
        }
        apply(group, request, code);
        group = groupRepository.save(group);
        // Data scope / limited changes affect every member's effective permissions.
        userRepository.bumpPrivVersionByGroupId(id);
        permissionService.evictAll();
        auditService.record("GROUP_UPDATE", "GROUP", id, before, toDto(group));
        return toDto(group);
    }

    @Transactional
    public void disable(Long id) {
        SysGroup group = requireGroup(id);
        if (userGroupRepository.countByGroupId(id) > 0) {
            throw new BusinessException(ErrorCode.GROUP_HAS_USERS);
        }
        GroupDto before = toDto(group);
        group.setStatus("DISABLED");
        groupRepository.save(group);
        permissionService.evictAll();
        auditService.record("GROUP_DISABLE", "GROUP", id, before, toDto(group));
    }

    @Transactional
    public void assignPrivs(Long groupId, List<Long> privIds) {
        SysGroup group = requireGroup(groupId);
        List<Long> before = groupPrivRepository.findPrivIdsByGroupId(groupId);
        List<Long> distinct = privIds == null ? List.of() : privIds.stream().distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysPriv> privs = privRepository.findByIdIn(distinct);
            long active = privs.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
            if (privs.size() != distinct.size() || active != distinct.size()) {
                throw new BusinessException(ErrorCode.PRIV_NOT_FOUND, "存在未注册或已废弃的权限点");
            }
        }
        groupPrivRepository.deleteByGroupId(groupId);
        groupPrivRepository.flush();
        Set<Long> unique = new HashSet<>(distinct);
        for (Long privId : unique) {
            SysGroupPriv relation = new SysGroupPriv();
            relation.setGroupId(groupId);
            relation.setPrivId(privId);
            groupPrivRepository.save(relation);
        }
        int affectedUsers = userRepository.bumpPrivVersionByGroupId(groupId);
        permissionService.evictAll();
        auditService.record("GROUP_ASSIGN_PRIV", "GROUP", groupId,
                java.util.Map.of("privIds", before, "groupCode", group.getCode()),
                java.util.Map.of("privIds", distinct, "affectedUsers", affectedUsers));
    }

    private void apply(SysGroup group, GroupRequest request, String code) {
        group.setCode(code);
        group.setName(request.name().trim());
        group.setRole(request.role() == null || request.role().isBlank() ? code : request.role().trim());
        group.setDataScope(request.dataScope() == null ? "SELF" : request.dataScope());
        group.setLimited(Boolean.TRUE.equals(request.limited()));
        group.setScopeType(request.scopeType() == null || request.scopeType().isBlank()
                ? "GLOBAL" : request.scopeType().trim());
        group.setScopeId(request.scopeId() == null ? 0L : request.scopeId());
        group.setSort(request.sort() == null ? 0 : request.sort());
        if (group.getStatus() == null) {
            group.setStatus("ACTIVE");
        }
    }

    private SysGroup requireGroup(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }

    private GroupDto toDto(SysGroup group) {
        return GroupDto.of(group, userGroupRepository.countByGroupId(group.getId()));
    }
}
