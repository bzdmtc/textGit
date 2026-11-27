package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.permission.dto.GroupDto;
import com.example.smart_learning.permission.dto.UserCreateRequest;
import com.example.smart_learning.permission.dto.UserDto;
import com.example.smart_learning.permission.dto.UserPermissionView;
import com.example.smart_learning.permission.dto.UserUpdateRequest;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysPosition;
import com.example.smart_learning.permission.entity.SysUserGroup;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysPositionRepository;
import com.example.smart_learning.permission.repository.SysUserGroupRepository;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Master
public class UserAdminService {

    private final SysUserRepository userRepository;
    private final SysGroupRepository groupRepository;
    private final SysUserGroupRepository userGroupRepository;
    private final SysPositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public UserAdminService(SysUserRepository userRepository,
                            SysGroupRepository groupRepository,
                            SysUserGroupRepository userGroupRepository,
                            SysPositionRepository positionRepository,
                            PasswordEncoder passwordEncoder,
                            PermissionService permissionService,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.positionRepository = positionRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<UserDto> page(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<SysUser> result = userRepository.search(keyword, pageable);
        List<UserDto> items = result.getContent().stream().map(this::toDto).toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return toDto(requireUser(id));
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        String account = request.account().trim();
        if (userRepository.existsByAccount(account)) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        if (request.phone() != null && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealname(request.realname());
        user.setNickname(request.nickname());
        user.setPosition(blankToNull(request.position()));
        user.setPhone(blankToNull(request.phone()));
        user.setEmail(blankToNull(request.email()));
        user.setStatus("ACTIVE");
        user.setPrivVersion(0L);
        user = userRepository.save(user);
        // 选职位后未显式指定分组时，默认带出该职位的权限分组（仍可在表单中调整）
        List<Long> effectiveGroupIds = request.groupIds();
        // 只有未传 groupIds 时才按职位兜底；显式传空数组表示管理员清空了分组
        if (effectiveGroupIds == null && user.getPosition() != null) {
            Long defaultGroupId = defaultGroupId(user.getPosition());
            if (defaultGroupId != null) {
                effectiveGroupIds = List.of(defaultGroupId);
            }
        }
        if (effectiveGroupIds != null && !effectiveGroupIds.isEmpty()) {
            replaceGroups(user.getId(), effectiveGroupIds);
        }
        auditService.record("USER_CREATE", "USER", user.getId(), null,
                java.util.Map.of("account", account,
                        "position", user.getPosition() == null ? "" : user.getPosition(),
                        "groupIds", effectiveGroupIds == null ? List.of() : effectiveGroupIds));
        return toDto(user);
    }

    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        UserDto before = toDto(user);
        if (request.phone() != null && !request.phone().equals(user.getPhone())
                && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        String oldPosition = user.getPosition();
        String newPosition = blankToNull(request.position());
        boolean positionChanged = !Objects.equals(oldPosition, newPosition);
        user.setRealname(request.realname());
        user.setNickname(request.nickname());
        user.setPosition(newPosition);
        user.setPhone(blankToNull(request.phone()));
        user.setEmail(blankToNull(request.email()));
        user.setAvatar(request.avatar());
        user = userRepository.save(user);

        List<Long> effectiveGroupIds = request.groupIds();
        if (effectiveGroupIds == null && positionChanged) {
            // 切换职位：移除旧职位的默认分组，加入新职位的默认分组；其他手动分组保留
            List<Long> currentGroupIds = userGroupRepository.findByUserId(id).stream()
                    .map(SysUserGroup::getGroupId)
                    .toList();
            List<Long> merged = new ArrayList<>(currentGroupIds);
            Long oldDefaultGroupId = defaultGroupId(oldPosition);
            if (oldDefaultGroupId != null) {
                merged.remove(oldDefaultGroupId);
            }
            Long newDefaultGroupId = defaultGroupId(newPosition);
            if (newDefaultGroupId != null && !merged.contains(newDefaultGroupId)) {
                merged.add(newDefaultGroupId);
            }
            if (!merged.equals(currentGroupIds)) {
                effectiveGroupIds = merged;
            }
        }
        if (effectiveGroupIds != null) {
            replaceGroups(id, effectiveGroupIds);
            userRepository.bumpPrivVersion(id);
            permissionService.evict(id);
        }
        auditService.record("USER_UPDATE", "USER", id, before, toDto(user));
        return toDto(user);
    }

    @Transactional
    public UserDto setStatus(Long id, String status) {
        SysUser user = requireUser(id);
        UserDto before = toDto(user);
        user.setStatus(status);
        user = userRepository.save(user);
        userRepository.bumpPrivVersion(id);
        permissionService.evict(id);
        auditService.record("USER_STATUS", "USER", id, before, toDto(user));
        return toDto(user);
    }

    @Transactional
    public UserDto assignGroups(Long id, List<Long> groupIds) {
        SysUser user = requireUser(id);
        UserDto before = toDto(user);
        replaceGroups(id, groupIds);
        userRepository.bumpPrivVersion(id);
        permissionService.evict(id);
        UserDto after = toDto(user);
        auditService.record("USER_ASSIGN_GROUP", "USER", id,
                java.util.Map.of("groups", before.groups().stream().map(GroupDto::code).toList()),
                java.util.Map.of("groups", after.groups().stream().map(GroupDto::code).toList()));
        return after;
    }

    @Transactional(readOnly = true)
    public UserPermissionView permissions(Long id) {
        SysUser user = requireUser(id);
        var snapshot = permissionService.getSnapshot(user);
        return new UserPermissionView(user.getId(), user.getAccount(), snapshot.rights(),
                snapshot.menus(), snapshot.dataScope(), snapshot.limited(), snapshot.privVersion());
    }

    private void replaceGroups(Long userId, List<Long> groupIds) {
        List<Long> distinct = groupIds == null ? List.of() : groupIds.stream().distinct().toList();
        List<SysGroup> groups = groupRepository.findAllById(distinct);
        if (groups.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        userGroupRepository.deleteByUserId(userId);
        userGroupRepository.flush();
        for (Long groupId : distinct) {
            SysUserGroup relation = new SysUserGroup();
            relation.setUserId(userId);
            relation.setGroupId(groupId);
            userGroupRepository.save(relation);
        }
    }

    private UserDto toDto(SysUser user) {
        List<Long> groupIds = userGroupRepository.findByUserId(user.getId()).stream()
                .map(SysUserGroup::getGroupId)
                .toList();
        List<GroupDto> groups = groupRepository.findAllById(groupIds).stream()
                .map(group -> GroupDto.of(group, 0))
                .toList();
        return UserDto.of(user, groups);
    }

    private Long defaultGroupId(String position) {
        if (position == null) {
            return null;
        }
        return positionRepository.findByNameAndStatus(position, "ACTIVE")
                .map(SysPosition::getGroupId)
                .orElse(null);
    }

    private SysUser requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
