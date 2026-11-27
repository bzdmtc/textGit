package com.example.smart_learning.exam.repository;

import com.example.smart_learning.exam.entity.WrongQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {

    Optional<WrongQuestion> findByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<WrongQuestion> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndMastered(Long userId, Boolean mastered);

    @Query("select w from WrongQuestion w where w.userId = :userId " +
            "and (:mastered is null or w.mastered = :mastered) " +
            "and (:subject is null or :subject = '' or w.questionId in " +
            "(select q.id from Question q where q.subject = :subject)) " +
            "order by w.lastWrongAt desc")
    Page<WrongQuestion> search(@Param("userId") Long userId,
                               @Param("subject") String subject,
                               @Param("mastered") Boolean mastered,
                               Pageable pageable);

    @Query("select w.questionId from WrongQuestion w, Question q where q.id = w.questionId " +
            "and w.userId = :userId and w.mastered = false and q.status = 'ACTIVE' " +
            "and (:subject is null or :subject = '' or q.subject = :subject) " +
            "order by w.lastWrongAt desc")
    List<Long> findPracticeQuestionIds(@Param("userId") Long userId,
                                       @Param("subject") String subject);
}
