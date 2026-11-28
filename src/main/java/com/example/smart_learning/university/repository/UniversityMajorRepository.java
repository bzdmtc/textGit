package com.example.smart_learning.university.repository;

import com.example.smart_learning.university.entity.UniversityMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UniversityMajorRepository extends JpaRepository<UniversityMajor, Long> {

    @Query("""
            select m.universityId as universityId, count(m) as total
            from UniversityMajor m
            where m.universityId in :universityIds and m.status = 'ACTIVE'
            group by m.universityId
            """)
    List<MajorCount> countActiveByUniversityIds(@Param("universityIds") Collection<Long> universityIds);

    interface MajorCount {
        Long getUniversityId();

        long getTotal();
    }

    List<UniversityMajor> findByUniversityIdOrderBySortAscIdAsc(Long universityId);

    List<UniversityMajor> findByUniversityIdAndStatusOrderBySortAscIdAsc(Long universityId, String status);

    Optional<UniversityMajor> findByIdAndUniversityId(Long id, Long universityId);

    int countByUniversityIdAndStatus(Long universityId, String status);

    boolean existsByUniversityIdAndNameAndDegreeType(Long universityId, String name, String degreeType);

    boolean existsByUniversityIdAndNameAndDegreeTypeAndIdNot(Long universityId, String name, String degreeType,
                                                             Long id);
}
