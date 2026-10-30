package com.ai.rag.service;

import com.ai.rag.entity.AppUser;
import com.ai.rag.entity.RefreshTokenSession;
import com.ai.rag.repository.AppUserRepository;
import com.ai.rag.repository.RefreshTokenSessionRepository;
import com.ai.rag.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final long ACCESS_TOKEN_SECONDS = 600;
    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(30);
    private static final Duration ROTATION_RETRY_WINDOW = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final AppUserRepository appUserRepository;
    private final JwtEncoder jwtEncoder;

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Transactional
    public IssuedTokens createSession(AuthenticatedUser user) {
        UUID sessionId = UUID.randomUUID();
        String refreshToken = sessionId + "." + randomSecret();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plus(REFRESH_TOKEN_LIFETIME);
        refreshTokenSessionRepository.save(new RefreshTokenSession(
                sessionId,
                appUserRepository.getReferenceById(user.id()),
                hash(refreshToken),
                expiresAt));
        return tokens(user, refreshToken, expiresAt);
    }

    @Transactional
    public IssuedTokens refresh(String refreshToken) {
        ParsedRefreshToken parsed = parse(refreshToken);
        RefreshTokenSession session = refreshTokenSessionRepository
                .findByIdForUpdate(parsed.sessionId())
                .orElseThrow(AuthTokenService::invalidRefreshToken);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)) {
            throw invalidRefreshToken();
        }

        String presentedHash = hash(refreshToken);
        String nextRefreshToken;
        if (constantTimeEquals(presentedHash, session.getTokenHash())) {
            nextRefreshToken = successor(refreshToken, parsed.sessionId());
            session.rotate(hash(nextRefreshToken), now);
        } else if (constantTimeEquals(presentedHash, session.getPreviousTokenHash())
                && session.getPreviousRotatedAt() != null
                && session.getPreviousRotatedAt().plus(ROTATION_RETRY_WINDOW).isAfter(now)) {
            nextRefreshToken = successor(refreshToken, parsed.sessionId());
        } else {
            throw invalidRefreshToken();
        }

        return tokens(toAuthenticatedUser(session.getUser()), nextRefreshToken, session.getExpiresAt());
    }

    @Transactional
    public void revoke(String refreshToken) {
        ParsedRefreshToken parsed = parse(refreshToken);
        refreshTokenSessionRepository.findByIdForUpdate(parsed.sessionId()).ifPresent(session -> {
            String presentedHash = hash(refreshToken);
            if (constantTimeEquals(presentedHash, session.getTokenHash())
                    || constantTimeEquals(presentedHash, session.getPreviousTokenHash())) {
                session.revoke(OffsetDateTime.now(ZoneOffset.UTC));
            }
        });
    }

    private IssuedTokens tokens(
            AuthenticatedUser user,
            String refreshToken,
            OffsetDateTime refreshExpiresAt) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(ACCESS_TOKEN_SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("erp-api")
                .subject(user.email())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .claim("uid", user.id())
                .claim("username", user.username())
                .claim("email", user.email())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        long refreshExpiresInSeconds = Duration.between(
                OffsetDateTime.now(ZoneOffset.UTC), refreshExpiresAt).getSeconds();
        return new IssuedTokens(
                user, accessToken, refreshToken, ACCESS_TOKEN_SECONDS, refreshExpiresInSeconds);
    }

    private String successor(String refreshToken, UUID sessionId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(jwtSecret), "HmacSHA256"));
            byte[] value = mac.doFinal(("refresh-rotation:" + sessionId + ":" + refreshToken)
                    .getBytes(StandardCharsets.UTF_8));
            return sessionId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to rotate refresh token.", exception);
        }
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token.", exception);
        }
    }

    private static boolean constantTimeEquals(String first, String second) {
        return first != null && second != null
                && MessageDigest.isEqual(first.getBytes(StandardCharsets.US_ASCII),
                        second.getBytes(StandardCharsets.US_ASCII));
    }

    private static ParsedRefreshToken parse(String token) {
        if (token == null) throw invalidRefreshToken();
        int separator = token.indexOf('.');
        if (separator <= 0 || separator == token.length() - 1) throw invalidRefreshToken();
        try {
            return new ParsedRefreshToken(UUID.fromString(token.substring(0, separator)));
        } catch (IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private static AuthenticatedUser toAuthenticatedUser(AppUser user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getProfile().getEmail());
    }

    private static BadCredentialsException invalidRefreshToken() {
        return new BadCredentialsException("Refresh token is invalid or expired.");
    }

    private record ParsedRefreshToken(UUID sessionId) {
    }

    public record IssuedTokens(
            AuthenticatedUser user,
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            long refreshExpiresInSeconds) {
    }
}