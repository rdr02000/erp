package com.sass.ramel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sass.ramel.entity.AppUser;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.security.AuthenticatedUser;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        AppUser user = appUserRepository.findByProfileEmailIgnoreCase(email.trim())
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        String profileEmail = user.getProfile().getEmail();
        return new AuthenticatedUser(user.getId(), user.getUsername(), profileEmail);
    }

    @Transactional
    public void changePassword(long userId, String currentPassword, String newPassword) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Current password is incorrect."));
        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
        validateNewPassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    private static void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
    }
}
