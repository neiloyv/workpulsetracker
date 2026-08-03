package com.workpulsetracker.server.security;

import com.workpulsetracker.server.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
public class AgentJwtService {

    private static final Logger logger = LoggerFactory.getLogger(AgentJwtService.class);
    private static final String schema = "public";

    public static final String CLAIM_WORKER_ID = "workerId";
    public static final String CLAIM_DEVICE_ID = "deviceId";
    public static final String CLAIM_ORGANIZATION_ID = "organizationId";
    public static final String CLAIM_HARDWARE_ID = "hardwareId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_AGENT = "agent";

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public AgentJwtService(AppProperties appProperties) {
        String secret = appProperties.getJwt().getSecret();
        if (StringUtils.isBlank(secret) || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = appProperties.getJwt().getExpirationSeconds();
    }

    public String issueToken(
            Long workerId,
            Long deviceId,
            Long organizationId,
            String hardwareId,
            String email
    ) {
        Instant nowInstant = Instant.now();
        Instant expirationInstant = nowInstant.plusSeconds(expirationSeconds);
        String token = Jwts.builder()
                .subject(String.valueOf(workerId))
                .claim(CLAIM_WORKER_ID, workerId)
                .claim(CLAIM_DEVICE_ID, deviceId)
                .claim(CLAIM_ORGANIZATION_ID, organizationId)
                .claim(CLAIM_HARDWARE_ID, hardwareId)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_AGENT)
                .issuedAt(Date.from(nowInstant))
                .expiration(Date.from(expirationInstant))
                .signWith(secretKey)
                .compact();
        logger.info(
                "schema={} Issued agent JWT: workerId={}, deviceId={}, hardwareId={}",
                schema,
                workerId,
                deviceId,
                hardwareId
        );
        return token;
    }

    public AgentDevicePrincipal parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_AGENT.equals(tokenType)) {
            throw new IllegalArgumentException("Unsupported token type");
        }

        Long workerId = toLong(claims.get(CLAIM_WORKER_ID));
        Long deviceId = toLong(claims.get(CLAIM_DEVICE_ID));
        Long organizationId = toLong(claims.get(CLAIM_ORGANIZATION_ID));
        String hardwareId = claims.get(CLAIM_HARDWARE_ID, String.class);
        String email = claims.get(CLAIM_EMAIL, String.class);

        if (Objects.isNull(workerId)
                || Objects.isNull(deviceId)
                || Objects.isNull(organizationId)
                || StringUtils.isBlank(hardwareId)
                || StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Incomplete agent token claims");
        }

        return new AgentDevicePrincipal(workerId, deviceId, organizationId, hardwareId, email);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private static Long toLong(Object claimValue) {
        if (Objects.isNull(claimValue)) {
            return null;
        }
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(claimValue.toString());
    }
}
