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

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admission_stat")
public class AdmissionStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer applicants = 0;

    @Column(nullable = false)
    private Integer admitted = 0;

    @Column(precision = 6, scale = 2)
    private BigDecimal ratio;

    @Column(name = "min_score")
    private Integer minScore;
}
