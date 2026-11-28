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
@Table(name = "score_line")
public class ScoreLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "degree_type", nullable = false, length = 16)
    private String degreeType;

    private Integer political;

    private Integer english;

    private Integer math;

    @Column(name = "major_score")
    private Integer majorScore;

    @Column(nullable = false)
    private Integer total;
}
