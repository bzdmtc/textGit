package com.example.smart_learning.security;

import com.example.smart_learning.permission.dto.PermissionSnapshot;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoginUser implements UserDetails {

    private final Long id;
    private final String account;
    private final String passwordHash;
    private final String status;
    private final PermissionSnapshot permissions;
    private final List<GrantedAuthority> authorities;

    public LoginUser(Long id, String account, String passwordHash, String status, PermissionSnapshot permissions) {
        this.id = id;
        this.account = account;
        this.passwordHash = passwordHash;
        this.status = status;
        this.permissions = permissions;
        this.authorities = buildAuthorities(permissions);
    }

    private List<GrantedAuthority> buildAuthorities(PermissionSnapshot snapshot) {
        List<GrantedAuthority> result = new ArrayList<>();
        snapshot.rights().forEach((module, methods) ->
                methods.forEach(method -> result.add(new SimpleGrantedAuthority(module + ":" + method))));
        snapshot.groupCodes().forEach(code -> result.add(new SimpleGrantedAuthority("ROLE_" + code)));
        return List.copyOf(result);
    }

    public Long getId() {
        return id;
    }

    public PermissionSnapshot getPermissions() {
        return permissions;
    }

    public boolean hasPriv(String module, String method) {
        return permissions.has(module, method);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return account;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}
