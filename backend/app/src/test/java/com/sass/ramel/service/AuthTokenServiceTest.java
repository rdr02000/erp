package com.sass.ramel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.RefreshTokenSession;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.repository.RefreshTokenSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

    private static final String JWT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final RefreshTokenSessionRepository refreshTokenSessionRepository =
            mock(RefreshTokenSessionRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private final AuthTokenService authTokenService = new AuthTokenService(
            refreshTokenSessionRepository, appUserRepository, jwtEncoder);

    @BeforeEach
    void setJwtSecret() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                authTokenService, "jwtSecret", JWT_SECRET);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-access-token");
        when(jwtEncoder.encode(any())).thenReturn(jwt);
    }

    @Test
    void retryingRefreshWithinGraceReturnsTheSameRotatedToken() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String oldRefreshToken = sessionId + ".original-secret";
        AppUser user = mock(AppUser.class);
        UserProfile profile = mock(UserProfile.class);
        when(user.getId()).thenReturn(7L);
        when(user.getUsername()).thenReturn("casey");
        when(user.getProfile()).thenReturn(profile);
        when(profile.getEmail()).thenReturn("casey@example.test");
        RefreshTokenSession session = new RefreshTokenSession(
                sessionId,
                user,
                hash(oldRefreshToken),
                OffsetDateTime.now().plusDays(30));
        when(refreshTokenSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        var firstResponse = authTokenService.refresh(oldRefreshToken);
        var retryResponse = authTokenService.refresh(oldRefreshToken);

        assertEquals(firstResponse.refreshToken(), retryResponse.refreshToken());
        assertEquals("signed-access-token", retryResponse.accessToken());
    }

    @Test
    void passwordOnlyRefreshSessionCannotRefreshAfterTotpIsEnabled() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String refreshToken = sessionId + ".original-secret";
        AppUser user = new AppUser("casey", "encoded-password");
        user.setTotpEnabled(true);
        UserProfile profile = new UserProfile(user);
        profile.setEmail("casey@example.test");
        user.setProfile(profile);
        RefreshTokenSession session = new RefreshTokenSession(
                sessionId,
                user,
                hash(refreshToken),
                OffsetDateTime.now().plusDays(30),
                false);
        when(refreshTokenSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        assertThrows(
                BadCredentialsException.class,
                () -> authTokenService.refresh(refreshToken));
    }

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}