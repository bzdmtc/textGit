package com.example.smart_learning.security;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.permission.dto.PermissionSnapshot;
import com.example.smart_learning.permission.service.PermissionService;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserRepository userRepository;
    private final PermissionService permissionService;

    public UserDetailsServiceImpl(SysUserRepository userRepository, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Override
    @Master
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByAccount(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return toLoginUser(user);
    }

    @Master
    @Transactional(readOnly = true)
    public LoginUser loadById(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return toLoginUser(user);
    }

    @Master
    @Transactional(readOnly = true)
    public LoginUser loadByPhone(String phone) {
        SysUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return toLoginUser(user);
    }

    @Master
    @Transactional(readOnly = true)
    public LoginUser loadByEmail(String email) {
        SysUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return toLoginUser(user);
    }

    private LoginUser toLoginUser(SysUser user) {
        PermissionSnapshot snapshot = permissionService.getSnapshot(user);
        return new LoginUser(user.getId(), user.getAccount(), user.getPasswordHash(), user.getStatus(), snapshot);
    }
}
