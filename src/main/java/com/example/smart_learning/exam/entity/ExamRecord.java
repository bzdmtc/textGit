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
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_record")
public class ExamRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "paper_id")
    private Long paperId;

    /** PRACTICE / EXAM */
    @Column(nullable = false, length = 16)
    private String mode;

    /** NORMAL / WRONG */
    @Column(nullable = false, length = 16)
    private String source = "NORMAL";

    @Column(length = 32)
    private String subject;

    @Column(length = 100)
    private String chapter;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(precision = 8, scale = 2)
    private BigDecimal score;

    @Column(name = "total_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount = 0;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount = 0;

    /** IN_PROGRESS / SUBMITTED */
    @Column(nullable = false, length = 16)
    private String status = "IN_PROGRESS";
}
