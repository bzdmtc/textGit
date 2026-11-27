package com.example.smart_learning.exam.repository;

import com.example.smart_learning.exam.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {

    List<PaperQuestion> findByPaperIdOrderBySortAscIdAsc(Long paperId);

    List<PaperQuestion> findByPaperIdIn(List<Long> paperIds);

    void deleteByPaperId(Long paperId);

    long countByQuestionId(Long questionId);

    long countByPaperId(Long paperId);
}
