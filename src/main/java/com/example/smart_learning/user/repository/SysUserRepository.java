package com.example.smart_learning.user.repository;

import com.example.smart_learning.user.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    @Query("select u from SysUser u where (:keyword is null or :keyword = '' " +
            "or lower(u.account) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(u.realname, '')) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(u.position, '')) like lower(concat('%', :keyword, '%')) " +
            "or coalesce(u.phone, '') like concat('%', :keyword, '%') " +
            "or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))) " +
            "order by u.id desc")
    Page<SysUser> search(@Param("keyword") String keyword, Pageable pageable);

    Optional<SysUser> findByAccount(String account);

    Optional<SysUser> findByPhone(String phone);

    Optional<SysUser> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByAccount(String account);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SysUser u set u.privVersion = u.privVersion + 1 " +
            "where u.id in (select ug.userId from SysUserGroup ug where ug.groupId = :groupId)")
    int bumpPrivVersionByGroupId(@Param("groupId") Long groupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SysUser u set u.privVersion = u.privVersion + 1 where u.id = :userId")
    int bumpPrivVersion(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SysUser u set u.position = :newName where u.position = :oldName")
    int renamePosition(@Param("oldName") String oldName, @Param("newName") String newName);
}
