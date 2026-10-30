package com.ai.rag.service;

import com.ai.rag.entity.AppUser;
import com.ai.rag.entity.RefreshTokenSession;
import com.ai.rag.entity.UserProfile;
import com.ai.rag.repository.AppUserRepository;
import com.ai.rag.repository.RefreshTokenSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}