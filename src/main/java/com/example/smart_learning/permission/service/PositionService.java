package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.permission.dto.PositionDto;
import com.example.smart_learning.permission.dto.PositionRequest;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysPosition;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysPositionRepository;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class PositionService {

    private final SysPositionRepository positionRepository;
    private final SysGroupRepository groupRepository;
    private final SysUserRepository userRepository;
    private final AuditService auditService;

    public PositionService(SysPositionRepository positionRepository,
                           SysGroupRepository groupRepository,
                           SysUserRepository userRepository,
                           AuditService auditService) {
        this.positionRepository = positionRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PositionDto> list() {
        List<SysPosition> positions = positionRepository.findAllByOrderBySortAscIdAsc();
        List<Long> groupIds = positions.stream().map(SysPosition::getGroupId).distinct().toList();
        Map<Long, SysGroup> groups = groupIds.isEmpty() ? Map.of()
                : groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(SysGroup::getId, Function.identity()));
        return positions.stream()
                .map(position -> PositionDto.of(position, groups.get(position.getGroupId())))
                .toList();
    }

    @Transactional
    public PositionDto create(PositionRequest request) {
        String name = request.name().trim();
        if (positionRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.POSITION_NAME_EXISTS);
        }
        SysGroup group = requireGroup(request.groupId());
        SysPosition position = new SysPosition();
        position.setName(name);
        position.setGroupId(group.getId());
        position.setStatus("ACTIVE");
        position.setSort(request.sort() == null ? 0 : request.sort());
        position = positionRepository.save(position);
        PositionDto dto = PositionDto.of(position, group);
        auditService.record("POSITION_CREATE", "POSITION", position.getId(), null, dto);
        return dto;
    }

    @Transactional
    public PositionDto update(Long id, PositionRequest request) {
        SysPosition position = requirePosition(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(position.getName())
                && positionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.POSITION_NAME_EXISTS);
        }
        SysGroup group = requireGroup(request.groupId());
        PositionDto before = PositionDto.of(position,
                groupRepository.findById(position.getGroupId()).orElse(null));
        String oldName = position.getName();
        position.setName(name);
        position.setGroupId(group.getId());
        if (request.sort() != null) {
            position.setSort(request.sort());
        }
        position = positionRepository.save(position);
        if (!oldName.equals(name)) {
            // 职位名称同时作为 sys_user.position 的展示值，改名时同步更新现有用户
            userRepository.renamePosition(oldName, name);
        }
        PositionDto dto = PositionDto.of(position, group);
        auditService.record("POSITION_UPDATE", "POSITION", id, before, dto);
        return dto;
    }

    @Transactional
    public PositionDto changeStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 ACTIVE/DISABLED");
        }
        SysPosition position = requirePosition(id);
        SysGroup group = groupRepository.findById(position.getGroupId()).orElse(null);
        PositionDto before = PositionDto.of(position, group);
        position.setStatus(normalized);
        position = positionRepository.save(position);
        PositionDto dto = PositionDto.of(position, group);
        auditService.record("POSITION_STATUS", "POSITION", id, before, dto);
        return dto;
    }

    private SysPosition requirePosition(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    }

    private SysGroup requireGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }
}
