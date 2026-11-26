package com.example.smart_learning.course.dto;

import com.example.smart_learning.course.entity.Teacher;

public record TeacherDto(
        Long id,
        String name,
        String title,
        String avatar,
        String subject,
        String introduction,
        Integer sort,
        String status,
        Long ownerUserId
) {

    public static TeacherDto of(Teacher teacher) {
        return new TeacherDto(teacher.getId(), teacher.getName(), teacher.getTitle(), teacher.getAvatar(),
                teacher.getSubject(), teacher.getIntroduction(), teacher.getSort(), teacher.getStatus(),
                teacher.getOwnerUserId());
    }
}
