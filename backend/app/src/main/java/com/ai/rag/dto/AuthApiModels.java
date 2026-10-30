package com.ai.rag.dto;

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
