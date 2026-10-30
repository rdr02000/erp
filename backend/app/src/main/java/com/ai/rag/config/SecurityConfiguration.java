package com.ai.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

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
                return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(
                                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
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
                                        if ("ERP_ACCESS_TOKEN".equals(cookie.getName())) return cookie.getValue();
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
                        var user = new com.ai.rag.security.AuthenticatedUser(
                                        id, jwt.getClaimAsString("username"), jwt.getClaimAsString("email"));
                    return UsernamePasswordAuthenticationToken.authenticated(
                            user, jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                };
        }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository,
            BearerTokenResolver bearerTokenResolver,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/rag/**"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/csrf",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/rag/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(403)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
            List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
