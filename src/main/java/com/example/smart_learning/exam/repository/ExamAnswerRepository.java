package com.example.smart_learning.exam.repository;

import com.example.smart_learning.exam.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByRecordIdOrderByIdAsc(Long recordId);

    Optional<ExamAnswer> findByRecordIdAndQuestionId(Long recordId, Long questionId);

    long countByRecordId(Long recordId);

    void deleteByRecordId(Long recordId);
}
