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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_group")
public class SysGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 500)
    private String description;

    /** 兼容旧数据的分组预设标签，不再对外展示；职位见 sys_user.position。 */
    @Column(length = 32)
    private String role;

    @Column(name = "data_scope", nullable = false, length = 16)
    private String dataScope = "SELF";

    @Column(nullable = false)
    private Boolean limited = false;

    @Column(name = "scope_type", nullable = false, length = 16)
    private String scopeType = "GLOBAL";

    @Column(name = "scope_id", nullable = false)
    private Long scopeId = 0L;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Integer sort = 0;
}
