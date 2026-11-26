package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysPriv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SysPrivRepository extends JpaRepository<SysPriv, Long> {

    Optional<SysPriv> findByModuleAndMethod(String module, String method);

    List<SysPriv> findAllByOrderByModuleAscSortAscIdAsc();

    List<SysPriv> findByStatusOrderByModuleAscSortAscIdAsc(String status);

    List<SysPriv> findByIdIn(Collection<Long> ids);
}
