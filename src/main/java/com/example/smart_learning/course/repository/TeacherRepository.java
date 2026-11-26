package com.example.smart_learning.course.repository;

import com.example.smart_learning.course.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    List<Teacher> findByStatusOrderBySortAscIdAsc(String status);

    List<Teacher> findAllByOrderBySortAscIdAsc();

    List<Teacher> findByIdIn(List<Long> ids);

    @Query("select t from Teacher t where (:keyword is null or :keyword = '' " +
            "or lower(t.name) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(t.title, '')) like lower(concat('%', :keyword, '%'))) " +
            "order by t.sort asc, t.id asc")
    List<Teacher> search(@Param("keyword") String keyword);
}
