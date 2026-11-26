package com.example.smart_learning.course.service;

import com.example.smart_learning.common.datasource.ReadOnly;
import com.example.smart_learning.course.dto.TeacherDto;
import com.example.smart_learning.course.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherPublicService {

    private final TeacherRepository teacherRepository;

    public TeacherPublicService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<TeacherDto> list() {
        return teacherRepository.findByStatusOrderBySortAscIdAsc("ACTIVE").stream()
                .map(TeacherDto::of)
                .toList();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public TeacherDto detail(Long id) {
        return teacherRepository.findById(id)
                .filter(teacher -> "ACTIVE".equals(teacher.getStatus()))
                .map(TeacherDto::of)
                .orElse(null);
    }
}
