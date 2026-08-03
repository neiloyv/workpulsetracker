package com.workpulsetracker.server.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal агента: worker + device из JWT, без пароля веб-аккаунта.
 */
public class AgentDevicePrincipal implements UserDetails {

    private final Long workerId;
    private final Long deviceId;
    private final Long organizationId;
    private final String hardwareId;
    private final String email;

    public AgentDevicePrincipal(
            Long workerId,
            Long deviceId,
            Long organizationId,
            String hardwareId,
            String email
    ) {
        this.workerId = workerId;
        this.deviceId = deviceId;
        this.organizationId = organizationId;
        this.hardwareId = hardwareId;
        this.email = email;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @Override
    public String getPassword() {
        return "";
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
        return true;
    }
}
