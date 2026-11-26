package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.permission.dto.MenuItem;
import com.example.smart_learning.permission.dto.PermissionSnapshot;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysPriv;
import com.example.smart_learning.permission.repository.SysGroupPrivRepository;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysPrivRepository;
import com.example.smart_learning.permission.repository.SysUserGroupRepository;
import com.example.smart_learning.user.entity.SysUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PermissionService {

    private final SysUserGroupRepository userGroupRepository;
    private final SysGroupRepository groupRepository;
    private final SysGroupPrivRepository groupPrivRepository;
    private final SysPrivRepository privRepository;

    private final Cache<Long, CachedSnapshot> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public PermissionService(SysUserGroupRepository userGroupRepository,
                             SysGroupRepository groupRepository,
                             SysGroupPrivRepository groupPrivRepository,
                             SysPrivRepository privRepository) {
        this.userGroupRepository = userGroupRepository;
        this.groupRepository = groupRepository;
        this.groupPrivRepository = groupPrivRepository;
        this.privRepository = privRepository;
    }

    @Master
    @Transactional(readOnly = true)
    public PermissionSnapshot getSnapshot(SysUser user) {
        long version = user.getPrivVersion() == null ? 0L : user.getPrivVersion();
        CachedSnapshot cached = cache.getIfPresent(user.getId());
        if (cached != null && cached.privVersion() == version) {
            return cached.snapshot();
        }
        PermissionSnapshot snapshot = loadSnapshot(user);
        cache.put(user.getId(), new CachedSnapshot(version, snapshot));
        return snapshot;
    }

    private PermissionSnapshot loadSnapshot(SysUser user) {
        List<Long> groupIds = userGroupRepository.findActiveGroupIdsByUserId(user.getId());
        if (groupIds.isEmpty()) {
            return new PermissionSnapshot(user.getPrivVersion(), Map.of(), List.of(), List.of(), "SELF", true);
        }
        List<SysGroup> groups = groupRepository.findAllById(groupIds);
        List<Long> privIds = groupPrivRepository.findPrivIdsByGroupIds(groupIds);
        List<SysPriv> privs = privIds.isEmpty() ? List.of() : privRepository.findByIdIn(privIds);

        Map<String, Set<String>> rights = new LinkedHashMap<>();
        List<MenuItem> menus = new ArrayList<>();
        for (SysPriv priv : privs) {
            if (!"ACTIVE".equals(priv.getStatus())) {
                continue;
            }
            rights.computeIfAbsent(priv.getModule(), key -> new LinkedHashSet<>()).add(priv.getMethod());
            if ("VIEW".equals(priv.getType()) && priv.getMenuPath() != null && !priv.getMenuPath().isBlank()) {
                menus.add(new MenuItem(priv.getModule(), priv.getMethod(), priv.getName(),
                        priv.getMenuPath(), priv.getIcon(), priv.getSort()));
            }
        }
        menus.sort(Comparator.comparingInt(MenuItem::sort));

        String dataScope = groups.stream()
                .map(SysGroup::getDataScope)
                .max(Comparator.comparingInt(PermissionService::scopeRank))
                .orElse("SELF");
        boolean allLimited = groups.stream().allMatch(g -> Boolean.TRUE.equals(g.getLimited()));
        List<String> groupCodes = groups.stream()
                .sorted(Comparator.comparing(SysGroup::getSort).thenComparing(SysGroup::getId))
                .map(SysGroup::getCode)
                .toList();

        Map<String, Set<String>> immutableRights = new LinkedHashMap<>();
        rights.forEach((module, methods) -> immutableRights.put(module, Set.copyOf(methods)));
        return new PermissionSnapshot(
                user.getPrivVersion() == null ? 0L : user.getPrivVersion(),
                Map.copyOf(immutableRights), List.copyOf(menus), List.copyOf(groupCodes), dataScope, allLimited);
    }

    private static int scopeRank(String scope) {
        return switch (scope == null ? "" : scope) {
            case "ALL" -> 3;
            case "OWN" -> 2;
            default -> 1;
        };
    }

    public void evict(Long userId) {
        cache.invalidate(userId);
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    private record CachedSnapshot(long privVersion, PermissionSnapshot snapshot) {
    }
}
