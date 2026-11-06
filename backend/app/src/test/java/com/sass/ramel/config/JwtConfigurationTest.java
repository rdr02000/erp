package com.sass.ramel.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.sass.ramel.security.AuthenticatedUser;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigurationTest {

    private static final String VALID_SECRET =
            Base64.getEncoder().encodeToString(new byte[32]);

    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void createsJwtKeyFromBase64SecretOfAtLeast32Bytes() {
        var key = configuration.jwtSecretKey(VALID_SECRET);

        assertEquals("HmacSHA256", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length);
    }

    @Test
    void rejectsJwtSecretShorterThan32Bytes() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        assertThrows(IllegalArgumentException.class, () -> configuration.jwtSecretKey(shortSecret));
    }

    @Test
    void rejectsMalformedBase64JwtSecret() {
        assertThrows(IllegalArgumentException.class, () -> configuration.jwtSecretKey("not-base64"));
    }

    @Test
    void jwtEncoderProducesTokenAcceptedByHs256Decoder() {
        var key = configuration.jwtSecretKey(VALID_SECRET);
        var encoder = configuration.jwtEncoder(key);
        var decoder = configuration.jwtDecoder(key);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("user@example.test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("uid", 42L)
                .build();

        Jwt decoded = decoder.decode(encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue());

        assertEquals("user@example.test", decoded.getSubject());
        assertEquals(42L, ((Number) decoded.getClaim("uid")).longValue());
    }

    @Test
    void bearerTokenResolverPrefersAccessCookieToAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("ERP_ACCESS_TOKEN", "cookie-token"));
        request.addHeader("Authorization", "Bearer header-token");

        assertEquals("cookie-token", configuration.bearerTokenResolver().resolve(request));
    }

    @Test
    void bearerTokenResolverFallsBackToBearerAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");

        assertEquals("header-token", configuration.bearerTokenResolver().resolve(request));
    }

    @Test
    void bearerTokenResolverReturnsNullWhenNoSupportedTokenIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic credentials");

        assertNull(configuration.bearerTokenResolver().resolve(request));
    }

    @Test
    void jwtAuthenticationConverterBuildsAuthenticatedUserWithUserRole() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .subject("jordan@example.test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("uid", 42L)
                .claim("username", "jordan")
                .claim("email", "jordan@example.test")
                .build();

        AbstractAuthenticationToken authentication =
                configuration.jwtAuthenticationConverter().convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        AuthenticatedUser user = assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        assertEquals(42L, user.id());
        assertEquals("jordan", user.username());
        assertEquals("jordan@example.test", user.email());
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
    }
}
