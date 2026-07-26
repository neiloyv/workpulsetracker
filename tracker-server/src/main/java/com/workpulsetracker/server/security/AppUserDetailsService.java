package com.workpulsetracker.server.security;

import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.repository.AppUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("Email is required");
        }
        AppUserEntity appUserEntity = appUserRepository.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (Objects.isNull(appUserEntity.getPasswordHash())) {
            throw new UsernameNotFoundException("User credentials are not configured");
        }
        return new AppUserPrincipal(
                appUserEntity.getId(),
                appUserEntity.getEmail(),
                appUserEntity.getPasswordHash(),
                appUserEntity.getRole()
        );
    }
}
