package com.example.smart_learning.course.entity;

import com.example.smart_learning.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teacher")
public class Teacher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String title;

    @Column(length = 500)
    private String avatar;

    @Column(length = 32)
    private String subject;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String introduction;

    @Column(nullable = false)
    private Integer sort = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "owner_user_id")
    private Long ownerUserId;
}
