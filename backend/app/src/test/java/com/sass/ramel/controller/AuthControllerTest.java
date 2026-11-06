package com.sass.ramel.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.sass.ramel.dto.AuthApiModels.ChangePasswordRequest;
import com.sass.ramel.dto.AuthApiModels.LoginRequest;
import com.sass.ramel.dto.AuthApiModels.TwoFactorLoginRequest;
import com.sass.ramel.security.AuthenticatedUser;
import com.sass.ramel.service.AuthService;
import com.sass.ramel.service.AuthTokenService;
import com.sass.ramel.service.LoginActivityService;
import com.sass.ramel.service.TwoFactorAuthenticationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final LoginActivityService loginActivityService = mock(LoginActivityService.class);
    private final TwoFactorAuthenticationService twoFactorAuthenticationService =
            mock(TwoFactorAuthenticationService.class);
    private final CookieCsrfTokenRepository csrfTokenRepository =
            mock(CookieCsrfTokenRepository.class);
    private final AuthController authController =
            new AuthController(
                    authService,
                    authTokenService,
                    loginActivityService,
                    twoFactorAuthenticationService,
                    csrfTokenRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
        void loginIssuesHttpOnlyTokensWithoutCreatingASession() {
        var user = new AuthenticatedUser(7, "casey", "casey@example.test");
                var tokens = new AuthTokenService.IssuedTokens(user, "access", "refresh", 600, 3600);
        when(authService.authenticate("casey@example.test", "correct-password"))
                .thenReturn(user);
                when(authTokenService.createSession(user)).thenReturn(tokens);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/129.0.0.0");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = authController.login(
                new LoginRequest("casey@example.test", "correct-password"),
                request,
                response);

        assertEquals(200, result.getStatusCode().value());
        assertNull(request.getSession(false));
        assertEquals(2, response.getHeaders("Set-Cookie").size());
        org.junit.jupiter.api.Assertions.assertTrue(
                response.getHeaders("Set-Cookie").stream().allMatch(value -> value.contains("HttpOnly")));
        verify(loginActivityService).recordSuccessfulLogin(
                7,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/129.0.0.0",
                null);
        verify(authTokenService).createSession(user);
    }

    @Test
    void invalidCredentialsReturnUnauthorizedWithoutCreatingASession() {
        when(authService.authenticate("casey@example.test", "wrong-password"))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid email or password."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = authController.login(
                new LoginRequest("casey@example.test", "wrong-password"),
                request,
                response);

        assertEquals(401, result.getStatusCode().value());
        assertNull(request.getSession(false));
        verify(loginActivityService, never()).recordSuccessfulLogin(any(Long.class), any(), any());
        verify(authTokenService, never()).createSession(any());
    }

    @Test
    void passwordLoginWithTotpReturnsChallengeWithoutIssuingSession() {
        var user = new AuthenticatedUser(7, "casey", "casey@example.test");
        when(authService.authenticate("casey@example.test", "correct-password"))
                .thenReturn(user);
        when(twoFactorAuthenticationService.isEnabled(7)).thenReturn(true);
        when(twoFactorAuthenticationService.createLoginChallenge(user))
                .thenReturn("challenge-token");

        var result = authController.login(
                new LoginRequest("casey@example.test", "correct-password"),
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertEquals(200, result.getStatusCode().value());
        assertEquals(
                new com.sass.ramel.dto.AuthApiModels.TwoFactorLoginChallengeResponse(
                        true, "challenge-token"),
                result.getBody());
        verify(authTokenService, never()).createSession(any());
        verify(loginActivityService, never()).recordSuccessfulLogin(any(Long.class), any(), any());
    }

    @Test
    void validSecondFactorCompletesLoginAndIssuesSession() {
        var user = new AuthenticatedUser(7, "casey", "casey@example.test");
        var request = new TwoFactorLoginRequest("challenge-token", "123456", null);
        var tokens = new AuthTokenService.IssuedTokens(user, "access", "refresh", 600, 3600);
        when(twoFactorAuthenticationService.verifyLoginChallenge("challenge-token", "123456"))
                .thenReturn(java.util.Optional.of(user));
        when(authTokenService.createSession(user, true)).thenReturn(tokens);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = authController.verifyTwoFactorLogin(
                request, new MockHttpServletRequest(), response);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(2, response.getHeaders("Set-Cookie").size());
        verify(authTokenService).createSession(user, true);
        verify(loginActivityService).recordSuccessfulLogin(7, null, null);
    }

    @Test
    void invalidSecondFactorDoesNotIssueSession() {
        when(twoFactorAuthenticationService.verifyLoginChallenge("challenge-token", "000000"))
                .thenReturn(java.util.Optional.empty());

        var result = authController.verifyTwoFactorLogin(
                new TwoFactorLoginRequest("challenge-token", "000000", null),
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertEquals(401, result.getStatusCode().value());
        verify(authTokenService, never()).createSession(any());
        verify(loginActivityService, never()).recordSuccessfulLogin(any(Long.class), any(), any());
    }

    @Test
    void changePasswordUsesTheAuthenticatedUserId() {
        var user = new AuthenticatedUser(7, "casey", "casey@example.test");

        var result = authController.changePassword(
                new ChangePasswordRequest("current-password", "new-password"),
                user);

        assertEquals(204, result.getStatusCode().value());
        verify(authService).changePassword(7, "current-password", "new-password");
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        var user = new AuthenticatedUser(7, "casey", "casey@example.test");
        doThrow(new org.springframework.security.authentication.BadCredentialsException(
                "Current password is incorrect."))
                .when(authService).changePassword(7, "wrong-password", "new-password");

        var result = authController.changePassword(
                new ChangePasswordRequest("wrong-password", "new-password"),
                user);

        assertEquals(400, result.getStatusCode().value());
    }

    @Test
    void rejectsInvalidLocationCoordinatesBeforeAuthenticating() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = authController.login(
                new LoginRequest(
                        "casey@example.test",
                        "correct-password",
                        new com.sass.ramel.dto.AuthApiModels.LoginLocation(100.0, 0.0, 10.0)),
                request,
                response);

        assertEquals(400, result.getStatusCode().value());
        verify(authService, never()).authenticate(any(), any());
        verify(loginActivityService, never()).recordSuccessfulLogin(any(Long.class), any(), any());
    }

    @Test
        void logoutRevokesRefreshSessionAndClearsCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
                request.setCookies(new jakarta.servlet.http.Cookie("ERP_REFRESH_TOKEN", "refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = authController.logout(request, response);

        assertEquals(204, result.getStatusCode().value());
        assertNull(request.getSession(false));
                verify(authTokenService).revoke("refresh");
                assertEquals(2, response.getHeaders("Set-Cookie").size());
        verify(csrfTokenRepository).saveToken(isNull(), same(request), same(response));
    }
}
