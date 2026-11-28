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
@Table(name = "university_major")
public class UniversityMajor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 32)
    private String code;

    /** 学硕 / 专硕 */
    @Column(name = "degree_type", nullable = false, length = 16)
    private String degreeType = "学硕";

    @Column(length = 16)
    private String duration;

    @Column(precision = 10, scale = 2)
    private BigDecimal tuition;

    @Column(name = "exam_subjects", length = 255)
    private String examSubjects;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String introduction;

    @Column(nullable = false)
    private Integer sort = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";
}
