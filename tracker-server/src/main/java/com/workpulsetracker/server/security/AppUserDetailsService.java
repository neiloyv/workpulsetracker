package com.workpulsetracker.server.security;

import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.repository.UserAccountRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public AppUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("Email is required");
        }
        UserAccountEntity userAccountEntity = userAccountRepository.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (Objects.isNull(userAccountEntity.getPasswordHash())) {
            throw new UsernameNotFoundException("User credentials are not configured");
        }
        return new UserAccountPrincipal(
                userAccountEntity.getId(),
                userAccountEntity.getEmail(),
                userAccountEntity.getPasswordHash(),
                userAccountEntity.getRole()
        );
    }
}
