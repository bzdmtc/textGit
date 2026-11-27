package com.example.smart_learning.exam.entity;

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

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper")
public class Paper extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** CHAPTER / PAST / MOCK */
    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 32)
    private String subject;

    @Column(length = 100)
    private String chapter;

    @Column(length = 1000)
    private String description;

    @Column(name = "total_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin = 0;

    /** DRAFT / PUBLISHED / OFF */
    @Column(nullable = false, length = 16)
    private String status = "DRAFT";

    @Column(name = "owner_user_id")
    private Long ownerUserId;
}
