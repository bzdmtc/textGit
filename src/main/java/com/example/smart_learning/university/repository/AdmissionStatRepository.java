package com.example.smart_learning.university.repository;

import com.example.smart_learning.university.entity.AdmissionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdmissionStatRepository extends JpaRepository<AdmissionStat, Long> {

    List<AdmissionStat> findByUniversityIdOrderByYearDescMajorIdAsc(Long universityId);

    List<AdmissionStat> findByMajorIdOrderByYearAsc(Long majorId);

    Optional<AdmissionStat> findByIdAndUniversityId(Long id, Long universityId);

    boolean existsByMajorIdAndYear(Long majorId, Integer year);

    boolean existsByMajorIdAndYearAndIdNot(Long majorId, Integer year, Long id);
}
