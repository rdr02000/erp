package com.ai.rag.controller;

import com.ai.rag.dto.AuthApiModels.AuthError;
import com.ai.rag.dto.AuthApiModels.LoginActivityView;
import com.ai.rag.dto.AuthApiModels.AuthUserResponse;
import com.ai.rag.dto.AuthApiModels.CsrfTokenResponse;
import com.ai.rag.dto.AuthApiModels.ChangePasswordRequest;
import com.ai.rag.dto.AuthApiModels.LoginRequest;
import com.ai.rag.security.AuthenticatedUser;
import com.ai.rag.service.AuthService;
import com.ai.rag.service.AuthTokenService;
import com.ai.rag.service.LoginActivityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokenService;
    private final LoginActivityService loginActivityService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    @Value("${app.auth.cookie-secure:false}")
    private boolean secureCookies;

    @Value("${app.auth.cookie-same-site:lax}")
    private String sameSite;

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new AuthError("Login request is required."));
        }
        if (!isValidLocation(request.location())) {
            return ResponseEntity.badRequest()
                    .body(new AuthError("Location coordinates are invalid."));
        }

        try {
            AuthenticatedUser user = authService.authenticate(
                    request.email(),
                    request.password());
            loginActivityService.recordSuccessfulLogin(
                    user.id(),
                    servletRequest.getHeader("User-Agent"),
                    request.location());
                writeTokens(authTokenService.createSession(user), servletResponse);

            return ResponseEntity.ok(toUserResponse(user));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid email or password."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            AuthTokenService.IssuedTokens tokens = authTokenService.refresh(
                    cookieValue(request, "ERP_REFRESH_TOKEN"));
            writeTokens(tokens, response);
            return ResponseEntity.ok(toUserResponse(tokens.user()));
        } catch (BadCredentialsException exception) {
            clearTokenCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Refresh token is invalid or expired."));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthError> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new AuthError("Password change request is required."));
        }
        try {
            authService.changePassword(user.id(), request.currentPassword(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (BadCredentialsException exception) {
            return ResponseEntity.badRequest().body(new AuthError("Current password is incorrect."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new AuthError(exception.getMessage()));
        }
    }

    @GetMapping("/me")
    public AuthUserResponse currentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toUserResponse(user);
    }

    @GetMapping("/activity")
    public List<LoginActivityView> recentLoginActivity(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return loginActivityService.getRecentActivity(user.id());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = cookieValue(request, "ERP_REFRESH_TOKEN");
            if (refreshToken != null) {
                try {
                    authTokenService.revoke(refreshToken);
                } catch (BadCredentialsException ignored) {
                    // Clear malformed cookies as well as valid refresh sessions.
                }
            }
        clearTokenCookies(response);
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    private void writeTokens(AuthTokenService.IssuedTokens tokens, HttpServletResponse response) {
        response.addHeader("Set-Cookie", tokenCookie(
                "ERP_ACCESS_TOKEN", tokens.accessToken(), "/api", tokens.accessExpiresInSeconds()));
        response.addHeader("Set-Cookie", tokenCookie(
                "ERP_REFRESH_TOKEN", tokens.refreshToken(), "/api/auth", tokens.refreshExpiresInSeconds()));
    }

    private void clearTokenCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", tokenCookie("ERP_ACCESS_TOKEN", "", "/api", 0));
        response.addHeader("Set-Cookie", tokenCookie("ERP_REFRESH_TOKEN", "", "/api/auth", 0));
    }

    private String tokenCookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .build()
                .toString();
    }

    private static String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private static AuthUserResponse toUserResponse(AuthenticatedUser user) {
        return new AuthUserResponse(user.id(), user.username(), user.email());
    }

    private static boolean isValidLocation(
            com.ai.rag.dto.AuthApiModels.LoginLocation location) {
        if (location == null) return true;
        if (location.latitude() == null || location.longitude() == null) return false;
        if (!Double.isFinite(location.latitude()) || !Double.isFinite(location.longitude())) {
            return false;
        }
        if (location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180) {
            return false;
        }
        return location.accuracyMeters() == null
                || Double.isFinite(location.accuracyMeters())
                && location.accuracyMeters() >= 0
                && location.accuracyMeters() <= 1_000_000;
    }
}
