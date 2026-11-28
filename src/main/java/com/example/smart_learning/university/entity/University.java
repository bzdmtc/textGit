package com.example.smart_learning.university.entity;

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
@Table(name = "university")
public class University extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 32)
    private String province;

    @Column(length = 32)
    private String city;

    /** 985 / 211 / 双一流 / 普通 */
    @Column(length = 32)
    private String level;

    /** 综合 / 理工 / 师范 / 财经 / 政法 */
    @Column(length = 32)
    private String type;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 255)
    private String website;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String introduction;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(nullable = false)
    private Integer sort = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "owner_user_id")
    private Long ownerUserId;
}
