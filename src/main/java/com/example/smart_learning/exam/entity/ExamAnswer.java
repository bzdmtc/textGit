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
@Table(name = "exam_answer")
public class ExamAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(length = 32)
    private String answer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    /** 题目快照 JSON，避免题目后续修改影响历史作答与成绩 */
    @Column(name = "question_snapshot", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionSnapshot;
}
