package com.example.smart_learning.question.repository;

import com.example.smart_learning.question.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderBySortAscIdAsc(Long questionId);

    List<QuestionOption> findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(List<Long> questionIds);

    void deleteByQuestionId(Long questionId);
}
