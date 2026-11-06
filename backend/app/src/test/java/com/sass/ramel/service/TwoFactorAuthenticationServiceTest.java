package com.sass.ramel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sass.ramel.dto.AuthApiModels.TwoFactorSetupResponse;
import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.TwoFactorLoginChallenge;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.repository.TwoFactorLoginChallengeRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoFactorAuthenticationServiceTest {

    private static final String JWT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final TwoFactorLoginChallengeRepository challengeRepository =
            mock(TwoFactorLoginChallengeRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TwoFactorAuthenticationService service =
            new TwoFactorAuthenticationService(
                    appUserRepository, challengeRepository, passwordEncoder);
    private final AppUser user = userWithEmail();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "jwtSecret", JWT_SECRET);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(appUserRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
    }

    @Test
    void generatesRfc6238CompatibleAuthenticatorCodes() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertEquals(
                "287082",
                TwoFactorAuthenticationService.codeForCounter(secret, 1));
        assertTrue(TwoFactorAuthenticationService.isValidCode(
                secret, "287082", Instant.ofEpochSecond(59)));
        assertFalse(TwoFactorAuthenticationService.isValidCode(
                secret, "287083", Instant.ofEpochSecond(59)));
    }

    @Test
    void acceptsCodesWithinNormalClockDriftWindow() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        long currentCounter = Instant.now().getEpochSecond() / 30;
        String driftCode = TwoFactorAuthenticationService.codeForCounter(secret, currentCounter + 2);

        assertTrue(TwoFactorAuthenticationService.isValidCode(
                secret,
                driftCode,
                Instant.ofEpochSecond((currentCounter + 2) * 30 + 5)));
    }

    @Test
    void encryptsPendingSecretAndEnablesTotpAfterValidCode() {
        TwoFactorSetupResponse setup =
                service.beginEnrollment(7L, "current-password");

        assertTrue(setup.provisioningUri().startsWith("otpauth://totp/"));
        assertFalse(user.getTotpPendingSecretEncrypted().contains(setup.secret()));

        String code = codeForCurrentTime(setup.secret());
        assertTrue(service.confirmEnrollment(7L, code));

        assertTrue(user.isTotpEnabled());
        assertTrue(user.getTotpSecretEncrypted() != null);
        assertTrue(user.getTotpPendingSecretEncrypted() == null);
        assertNotEquals(setup.secret(), user.getTotpSecretEncrypted());
        verify(appUserRepository, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void successfulLoginChallengeCanOnlyBeUsedOnce() {
        TwoFactorSetupResponse setup =
                service.beginEnrollment(7L, "current-password");
        assertTrue(service.confirmEnrollment(7L, codeForCurrentTime(setup.secret())));
        when(challengeRepository.countByUser_IdAndExpiresAtAfter(eq(7L), any()))
                .thenReturn(0L);

        String challengeToken = service.createLoginChallenge(
                new com.sass.ramel.security.AuthenticatedUser(7L, "casey", "casey@example.test"));
        var savedChallenge = org.mockito.ArgumentCaptor.forClass(TwoFactorLoginChallenge.class);
        verify(challengeRepository).save(savedChallenge.capture());
        when(challengeRepository.findByChallengeHashForUpdate(anyString()))
                .thenReturn(Optional.of(savedChallenge.getValue()));

        var verified = service.verifyLoginChallenge(
                challengeToken, codeForCurrentTime(setup.secret()));

        assertTrue(verified.isPresent());
        assertEquals("casey@example.test", verified.orElseThrow().email());
        assertTrue(service.verifyLoginChallenge(
                challengeToken, codeForCurrentTime(setup.secret())).isEmpty());
    }

    @Test
    void enrollmentSecretIsDiscardedAfterFiveInvalidCodes() {
        service.beginEnrollment(7L, "current-password");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(service.confirmEnrollment(7L, "invalid"));
        }

        assertTrue(user.getTotpPendingSecretEncrypted() == null);
        assertFalse(user.isTotpEnabled());
    }

    @Test
    void disablingTotpIsTemporarilyLockedAfterFiveInvalidCodes() {
        TwoFactorSetupResponse setup =
                service.beginEnrollment(7L, "current-password");
        assertTrue(service.confirmEnrollment(7L, codeForCurrentTime(setup.secret())));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(service.disable(7L, "current-password", "invalid"));
        }

        assertTrue(user.isTotpEnabled());
        assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.disable(7L, "current-password", "invalid"));
    }

    private static String codeForCurrentTime(String secret) {
        return TwoFactorAuthenticationService.codeForCounter(
                secret, Instant.now().getEpochSecond() / 30);
    }

    private static AppUser userWithEmail() {
        AppUser user = new AppUser("casey", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 7L);
        UserProfile profile = new UserProfile(user);
        profile.setEmail("casey@example.test");
        user.setProfile(profile);
        return user;
    }
}
