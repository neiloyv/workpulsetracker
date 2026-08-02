package com.workpulsetracker.server.security;

import com.workpulsetracker.server.enums.EntityStatus;
import com.workpulsetracker.server.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserAccountPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final EntityStatus status;

    public UserAccountPrincipal(
            Long id,
            String email,
            String passwordHash,
            UserRole role,
            EntityStatus status
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
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
        return status == EntityStatus.ACTIVE;
    }
}
