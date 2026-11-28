package com.example.smart_learning.university.repository;

import com.example.smart_learning.university.entity.University;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {

    @Query(value = """
            select u from University u
            where (:status is null or u.status = :status)
              and (:keyword is null or lower(u.name) like lower(concat('%', :keyword, '%')))
              and (:province is null or u.province = :province)
              and (:level is null or u.level = :level)
              and (:type is null or u.type = :type)
            order by u.sort asc, u.id asc
            """,
            countQuery = """
            select count(u) from University u
            where (:status is null or u.status = :status)
              and (:keyword is null or lower(u.name) like lower(concat('%', :keyword, '%')))
              and (:province is null or u.province = :province)
              and (:level is null or u.level = :level)
              and (:type is null or u.type = :type)
            """)
    Page<University> search(@Param("keyword") String keyword,
                            @Param("province") String province,
                            @Param("level") String level,
                            @Param("type") String type,
                            @Param("status") String status,
                            Pageable pageable);

    List<University> findTop8ByStatusAndFeaturedTrueOrderBySortAscIdAsc(String status);

    Optional<University> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("select distinct u.province from University u where u.status = 'ACTIVE' order by u.province")
    List<String> distinctProvinces();

    @Query("select distinct u.level from University u where u.status = 'ACTIVE' and u.level is not null order by u.level")
    List<String> distinctLevels();

    @Query("select distinct u.type from University u where u.status = 'ACTIVE' and u.type is not null order by u.type")
    List<String> distinctTypes();
}
