package com.sass.ramel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.repository.AppUserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DEMO_PASSWORD_HASH =
            "$2a$10$htAB3o9AwIZWyUBpmpXl9ulCbcA.xLJ2M5QP9TX4.jLXTFb8NzwYe";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticatesAfterTrimmingTheEmail() {
        AppUser user = spy(userWithEmail("casey@example.test"));
        doReturn(7L).when(user).getId();
        when(appUserRepository.findByProfileEmailIgnoreCase("CASEY@example.test"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        var authenticatedUser = authService.authenticate(
                "  CASEY@example.test  ",
                "correct-password");

        assertEquals(user.getUsername(), authenticatedUser.username());
        assertEquals("casey@example.test", authenticatedUser.email());
        verify(passwordEncoder).matches("correct-password", "encoded-password");
    }

    @Test
    void rejectsUnknownEmailOrIncorrectPasswordWithSameError() {
        when(appUserRepository.findByProfileEmailIgnoreCase("unknown@example.test"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate("unknown@example.test", "wrong-password"));
    }

    @Test
    void rejectsIncorrectPasswordForAnExistingEmail() {
        when(appUserRepository.findByProfileEmailIgnoreCase("casey@example.test"))
                .thenReturn(Optional.of(userWithEmail("casey@example.test")));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate("casey@example.test", "wrong-password"));
    }

    @Test
    void rejectsBlankCredentials() {
        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate("  ", "password"));
    }

        @Test
        void changesPasswordAfterVerifyingCurrentPassword() {
                AppUser user = spy(userWithEmail("casey@example.test"));
                when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
                when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");

                authService.changePassword(7L, "current-password", "new-password");

                assertEquals("new-password-hash", user.getPasswordHash());
                verify(appUserRepository).save(user);
        }

        @Test
        void rejectsPasswordChangeWhenCurrentPasswordIsIncorrect() {
                AppUser user = spy(userWithEmail("casey@example.test"));
                when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

                assertThrows(
                                BadCredentialsException.class,
                                () -> authService.changePassword(7L, "wrong-password", "new-password"));

                verify(passwordEncoder, never()).encode("new-password");
                verify(appUserRepository, never()).save(user);
        }

        @Test
        void rejectsNewPasswordThatDoesNotMeetPasswordPolicy() {
                AppUser user = spy(userWithEmail("casey@example.test"));
                when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.changePassword(7L, "current-password", "short"));

                verify(passwordEncoder, never()).encode("short");
                verify(appUserRepository, never()).save(user);
        }

    @Test
    void seededDemoPasswordHashMatchesTheDocumentedPassword() {
        assertTrue(new BCryptPasswordEncoder().matches("password", DEMO_PASSWORD_HASH));
    }

    private static AppUser userWithEmail(String email) {
        AppUser user = new AppUser("casey", "encoded-password");
        UserProfile profile = new UserProfile(user);
        profile.setEmail(email);
        user.setProfile(profile);
        return user;
    }
}
