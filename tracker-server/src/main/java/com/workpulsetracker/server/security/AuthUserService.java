package com.workpulsetracker.server.security;

import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.AppUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthUserService {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserService.class);

    private final AppUserRepository appUserRepository;

    public AuthUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUserEntity resolveOrCreateFromOAuth(OAuth2User oAuth2User) {
        String googleSub = oAuth2User.getName();
        String email = Objects.requireNonNullElse(oAuth2User.getAttribute("email"), "").toString();
        String givenName = optionalAttribute(oAuth2User, "given_name");
        String familyName = optionalAttribute(oAuth2User, "family_name");

        if (StringUtils.isBlank(email)) {
            throw new IllegalStateException("Google account email is required");
        }

        Optional<AppUserEntity> byGoogleSub = appUserRepository.findByGoogleSub(googleSub);
        if (byGoogleSub.isPresent()) {
            return byGoogleSub.get();
        }

        Optional<AppUserEntity> byEmail = appUserRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            AppUserEntity existingAppUser = byEmail.get();
            if (StringUtils.isBlank(existingAppUser.getGoogleSub())) {
                existingAppUser.setGoogleSub(googleSub);
                if (StringUtils.isBlank(existingAppUser.getFirstName())) {
                    existingAppUser.setFirstName(givenName);
                }
                if (StringUtils.isBlank(existingAppUser.getLastName())) {
                    existingAppUser.setLastName(familyName);
                }
                logger.info("Linked Google account to invited user {}", existingAppUser.getEmail());
                return appUserRepository.save(existingAppUser);
            }
            return existingAppUser;
        }

        AppUserEntity appUserEntity = new AppUserEntity(
                UUID.randomUUID(),
                null,
                googleSub,
                email.toLowerCase(),
                givenName,
                familyName,
                UserRole.MEMBER,
                false,
                OffsetDateTime.now()
        );
        AppUserEntity createdAppUser = appUserRepository.save(appUserEntity);
        logger.info("Created app user from Google login: {}", createdAppUser.getEmail());
        return createdAppUser;
    }

    public AppUserEntity requireCurrentUser(OAuth2AuthenticationToken authenticationToken) {
        String googleSub = authenticationToken.getPrincipal().getName();
        return appUserRepository.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private static String optionalAttribute(OAuth2User oAuth2User, String attributeName) {
        Object attributeValue = oAuth2User.getAttribute(attributeName);
        if (Objects.isNull(attributeValue)) {
            return null;
        }
        String value = attributeValue.toString();
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }
}
