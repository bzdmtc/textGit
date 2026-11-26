package com.example.smart_learning.user.entity;

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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String account;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 64)
    private String realname;

    @Column(length = 64)
    private String nickname;

    @Column(length = 500)
    private String avatar;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "priv_version", nullable = false)
    private Long privVersion = 0L;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
