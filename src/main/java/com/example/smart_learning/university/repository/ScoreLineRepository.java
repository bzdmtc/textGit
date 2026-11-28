package com.example.smart_learning.university.repository;

import com.example.smart_learning.university.entity.ScoreLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreLineRepository extends JpaRepository<ScoreLine, Long> {

    List<ScoreLine> findByUniversityIdOrderByYearDescMajorIdAsc(Long universityId);

    List<ScoreLine> findByMajorIdOrderByYearAsc(Long majorId);

    Optional<ScoreLine> findByIdAndUniversityId(Long id, Long universityId);

    boolean existsByMajorIdAndYear(Long majorId, Integer year);

    boolean existsByMajorIdAndYearAndIdNot(Long majorId, Integer year, Long id);
}
