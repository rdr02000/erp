package com.sass.ramel.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sass.ramel.security.AuthenticatedUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class JwtConfiguration {

    private static final String ACCESS_TOKEN_COOKIE = "ERP_ACCESS_TOKEN";

    @Bean
    SecretKey jwtSecretKey(@Value("${app.auth.jwt-secret}") String secret) {
        byte[] key = Base64.getDecoder().decode(secret);
        if (key.length < 32) {
            throw new IllegalArgumentException("APP_AUTH_JWT_SECRET must encode at least 32 bytes.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        return request -> {
            var cookies = request.getCookies();
            if (cookies != null) {
                for (var cookie : cookies) {
                    if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }

            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                return authorization.substring(7);
            }
            return null;
        };
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            long id = ((Number) jwt.getClaim("uid")).longValue();
            AuthenticatedUser user = new AuthenticatedUser(
                    id, jwt.getClaimAsString("username"), jwt.getClaimAsString("email"));
            return UsernamePasswordAuthenticationToken.authenticated(
                    user, jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        };
    }
}
