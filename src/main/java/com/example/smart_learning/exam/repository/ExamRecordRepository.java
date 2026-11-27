package com.example.smart_learning.exam.repository;

import com.example.smart_learning.exam.entity.ExamRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {

    Optional<ExamRecord> findByIdAndUserId(Long id, Long userId);

    Page<ExamRecord> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    List<ExamRecord> findTop5ByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("select coalesce(sum(r.score), 0), coalesce(sum(r.totalScore), 0), " +
            "coalesce(sum(r.correctCount), 0), coalesce(sum(r.questionCount), 0) " +
            "from ExamRecord r where r.userId = :userId and r.status = 'SUBMITTED'")
    List<Object[]> summarize(@Param("userId") Long userId);

    @Query("select count(r) from ExamRecord r where r.paperId = :paperId and r.status = 'IN_PROGRESS'")
    long countInProgressByPaper(@Param("paperId") Long paperId);
}
