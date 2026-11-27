package com.example.smart_learning.permission.entity;

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

/** 职位字典：职位对应一个默认权限分组，真正的权限仍由用户实际分组并集决定。 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_position")
public class SysPosition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String name;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Integer sort = 0;
}
