package com.sass.ramel.dto;

public final class AuthApiModels {

    private AuthApiModels() {
    }

    public record LoginRequest(String email, String password, LoginLocation location) {
        public LoginRequest(String email, String password) {
            this(email, password, null);
        }
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    public record LoginLocation(
            Double latitude,
            Double longitude,
            Double accuracyMeters) {
    }

    public record AuthUserResponse(long id, String username, String email) {
    }

    public record TwoFactorLoginChallengeResponse(
            boolean requiresTwoFactor,
            String challengeToken) {
    }

    public record TwoFactorLoginRequest(
            String challengeToken,
            String code,
            LoginLocation location) {
    }

    public record TwoFactorStatus(boolean enabled) {
    }

    public record TwoFactorSetupRequest(String currentPassword) {
    }

    public record TwoFactorSetupResponse(String secret, String provisioningUri) {
    }

    public record TwoFactorCodeRequest(String code) {
    }

    public record DisableTwoFactorRequest(String currentPassword, String code) {
    }

    public record LoginActivityView(
            long id,
            String deviceType,
            String browser,
            String operatingSystem,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            java.time.OffsetDateTime loggedAt) {
    }

    public record CsrfTokenResponse(String token) {
    }

    public record AuthError(String message) {
    }
}
