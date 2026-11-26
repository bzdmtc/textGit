package com.example.smart_learning.course.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.dto.TeacherRequest;
import com.example.smart_learning.course.entity.Teacher;
import com.example.smart_learning.course.repository.TeacherRepository;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.permission.service.DataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Master
public class TeacherAdminService {

    private final TeacherRepository teacherRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;

    public TeacherAdminService(TeacherRepository teacherRepository,
                               DataScopeService dataScopeService,
                               AuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TeacherDto> list(String keyword) {
        boolean all = dataScopeService.isAllScope();
        Long userId = dataScopeService.currentUserId();
        return teacherRepository.search(keyword).stream()
                .filter(teacher -> all || userId.equals(teacher.getOwnerUserId()))
                .map(TeacherDto::of)
                .toList();
    }

    @Transactional
    public TeacherDto create(TeacherRequest request) {
        Teacher teacher = new Teacher();
        teacher.setOwnerUserId(dataScopeService.currentUserId());
        apply(teacher, request);
        teacher = teacherRepository.save(teacher);
        auditService.record("TEACHER_CREATE", "TEACHER", teacher.getId(), null, TeacherDto.of(teacher));
        return TeacherDto.of(teacher);
    }

    @Transactional
    public TeacherDto update(Long id, TeacherRequest request) {
        Teacher teacher = require(id);
        dataScopeService.assertOwnerAccess(teacher.getOwnerUserId());
        TeacherDto before = TeacherDto.of(teacher);
        apply(teacher, request);
        teacher = teacherRepository.save(teacher);
        auditService.record("TEACHER_UPDATE", "TEACHER", id, before, TeacherDto.of(teacher));
        return TeacherDto.of(teacher);
    }

    @Transactional
    public void disable(Long id) {
        Teacher teacher = require(id);
        dataScopeService.assertOwnerAccess(teacher.getOwnerUserId());
        TeacherDto before = TeacherDto.of(teacher);
        teacher.setStatus("DISABLED");
        teacherRepository.save(teacher);
        auditService.record("TEACHER_DISABLE", "TEACHER", id, before, TeacherDto.of(teacher));
    }

    private void apply(Teacher teacher, TeacherRequest request) {
        teacher.setName(request.name().trim());
        teacher.setTitle(request.title());
        teacher.setAvatar(request.avatar());
        teacher.setSubject(request.subject());
        teacher.setIntroduction(request.introduction());
        teacher.setSort(request.sort() == null ? 0 : request.sort());
        teacher.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }

    private Teacher require(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "讲师不存在"));
    }
}
