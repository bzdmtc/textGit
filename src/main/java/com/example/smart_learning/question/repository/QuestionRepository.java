package com.example.smart_learning.question.repository;

import com.example.smart_learning.question.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("select q from Question q where " +
            "(:keyword is null or :keyword = '' " +
            "or lower(q.stem) like lower(concat('%', :keyword, '%')) " +
            "or lower(q.chapter) like lower(concat('%', :keyword, '%'))) " +
            "and (:subject is null or :subject = '' or q.subject = :subject) " +
            "and (:type is null or :type = '' or q.type = :type) " +
            "and (:status is null or :status = '' or q.status = :status) " +
            "and (:ownerId is null or q.ownerUserId = :ownerId) " +
            "order by q.id desc")
    Page<Question> search(@Param("keyword") String keyword,
                          @Param("subject") String subject,
                          @Param("type") String type,
                          @Param("status") String status,
                          @Param("ownerId") Long ownerId,
                          Pageable pageable);

    @Query("select q from Question q where q.status = 'ACTIVE' and q.subject = :subject " +
            "and (:chapter is null or :chapter = '' or q.chapter = :chapter) " +
            "order by q.id asc")
    List<Question> findPracticePool(@Param("subject") String subject,
                                    @Param("chapter") String chapter,
                                    Pageable pageable);

    List<Question> findByIdInAndStatus(List<Long> ids, String status);

    @Query("select q.subject, q.chapter, count(q) from Question q " +
            "where q.status = 'ACTIVE' group by q.subject, q.chapter order by q.subject, q.chapter")
    List<Object[]> countBySubjectChapter();

    long countByStatus(String status);
}
