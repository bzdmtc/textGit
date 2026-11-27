package com.example.smart_learning.question.entity;

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
@Table(name = "question")
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String subject;

    @Column(nullable = false, length = 100)
    private String chapter;

    /** SINGLE / MULTIPLE / JUDGE */
    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 2000)
    private String stem;

    @Column(length = 4000)
    private String analysis;

    @Column(nullable = false)
    private Integer difficulty = 3;

    /** ACTIVE / DISABLED */
    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "owner_user_id")
    private Long ownerUserId;
}
