package com.sass.ramel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sass.ramel.dto.AuthApiModels.TwoFactorSetupResponse;
import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.TwoFactorLoginChallenge;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.repository.TwoFactorLoginChallengeRepository;
import com.sass.ramel.security.AuthenticatedUser;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthenticationService {

    private static final String ISSUER = "ERP";
    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int ALLOWED_TIME_STEPS = 2;
    private static final int MAX_CHALLENGE_ATTEMPTS = 5;
    private static final int MAX_CHALLENGES_PER_WINDOW = 5;
    private static final int MAX_ENROLLMENT_ATTEMPTS = 5;
    private static final int MAX_DISABLE_ATTEMPTS = 5;
    private static final Duration CHALLENGE_LIFETIME = Duration.ofMinutes(5);
    private static final Duration DISABLE_LOCK_DURATION = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final TwoFactorLoginChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Transactional(readOnly = true)
    public boolean isEnabled(long userId) {
        return findUser(userId).isTotpEnabled();
    }

    @Transactional
    public TwoFactorSetupResponse beginEnrollment(long userId, String currentPassword) {
        AppUser user = findUserForUpdate(userId);
        requireCurrentPassword(user, currentPassword);
        if (user.isTotpEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Two-factor authentication is already enabled.");
        }

        byte[] secretBytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(secretBytes);
        String secret = encodeBase32(secretBytes);
        user.setTotpPendingSecretEncrypted(encrypt(secret));
        user.setTotpPendingFailedAttempts(0);
        appUserRepository.save(user);

        String label = URLEncoder.encode(
                ISSUER + ":" + user.getProfile().getEmail(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        String provisioningUri = "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
        return new TwoFactorSetupResponse(secret, provisioningUri);
    }

    @Transactional
    public boolean confirmEnrollment(long userId, String code) {
        AppUser user = findUserForUpdate(userId);
        String encryptedSecret = user.getTotpPendingSecretEncrypted();
        if (encryptedSecret == null) {
            return false;
        }
        String secret = decrypt(encryptedSecret);
        if (!isValidCode(secret, code, Instant.now())) {
            int failedAttempts = user.getTotpPendingFailedAttempts() + 1;
            if (failedAttempts >= MAX_ENROLLMENT_ATTEMPTS) {
                user.setTotpPendingSecretEncrypted(null);
                user.setTotpPendingFailedAttempts(0);
            } else {
                user.setTotpPendingFailedAttempts(failedAttempts);
            }
            appUserRepository.save(user);
            return false;
        }
        user.setTotpSecretEncrypted(encryptedSecret);
        user.setTotpPendingSecretEncrypted(null);
        user.setTotpPendingFailedAttempts(0);
        user.setTotpEnabled(true);
        appUserRepository.save(user);
        return true;
    }

    @Transactional
    public boolean disable(long userId, String currentPassword, String code) {
        AppUser user = findUserForUpdate(userId);
        requireCurrentPassword(user, currentPassword);
        if (!user.isTotpEnabled() || user.getTotpSecretEncrypted() == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (user.getTotpDisableLockedUntil() != null
                && user.getTotpDisableLockedUntil().isAfter(now)) {
            throw new BadCredentialsException(
                    "Two-factor disable is temporarily locked. Try again later.");
        }
        if (!isValidCode(
                decrypt(user.getTotpSecretEncrypted()), code, now.toInstant())) {
            int failedAttempts = user.getTotpDisableFailedAttempts() + 1;
            if (failedAttempts >= MAX_DISABLE_ATTEMPTS) {
                user.setTotpDisableFailedAttempts(0);
                user.setTotpDisableLockedUntil(now.plus(DISABLE_LOCK_DURATION));
            } else {
                user.setTotpDisableFailedAttempts(failedAttempts);
            }
            appUserRepository.save(user);
            return false;
        }
        user.setTotpEnabled(false);
        user.setTotpSecretEncrypted(null);
        user.setTotpPendingSecretEncrypted(null);
        user.setTotpPendingFailedAttempts(0);
        user.setTotpDisableFailedAttempts(0);
        user.setTotpDisableLockedUntil(null);
        appUserRepository.save(user);
        return true;
    }

    @Transactional
    public String createLoginChallenge(AuthenticatedUser authenticatedUser) {
        AppUser user = findUser(authenticatedUser.id());
        if (!user.isTotpEnabled() || user.getTotpSecretEncrypted() == null) {
            throw new BadCredentialsException("The authentication challenge is invalid.");
        }
        String token = randomToken();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        challengeRepository.deleteExpiredBefore(now);
        if (challengeRepository.countByUser_IdAndExpiresAtAfter(user.getId(), now)
                >= MAX_CHALLENGES_PER_WINDOW) {
            throw new BadCredentialsException(
                    "The authentication challenge is temporarily unavailable.");
        }
        challengeRepository.save(new TwoFactorLoginChallenge(
                UUID.randomUUID(),
                user,
                hash(token),
                now.plus(CHALLENGE_LIFETIME)));
        return token;
    }

    @Transactional
    public Optional<AuthenticatedUser> verifyLoginChallenge(String challengeToken, String code) {
        if (challengeToken == null || challengeToken.length() > 128) {
            return Optional.empty();
        }
        TwoFactorLoginChallenge challenge = challengeRepository
                .findByChallengeHashForUpdate(hash(challengeToken))
                .orElse(null);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (challenge == null
                || challenge.getConsumedAt() != null
                || !challenge.getExpiresAt().isAfter(now)
                || challenge.getFailedAttempts() >= MAX_CHALLENGE_ATTEMPTS) {
            return Optional.empty();
        }

        AppUser user = challenge.getUser();
        if (!user.isTotpEnabled()
                || user.getTotpSecretEncrypted() == null
                || !isValidCode(decrypt(user.getTotpSecretEncrypted()), code, now.toInstant())) {
            challenge.recordFailedAttempt(now, MAX_CHALLENGE_ATTEMPTS);
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        challenge.consume(now);
        challengeRepository.save(challenge);
        return Optional.of(new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getProfile().getEmail()));
    }

    static boolean isValidCode(String secret, String code, Instant now) {
        if (secret == null || code == null || !code.matches("[0-9]{6}")) {
            return false;
        }
        long currentCounter = now.getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -ALLOWED_TIME_STEPS; offset <= ALLOWED_TIME_STEPS; offset++) {
            String expected = codeForCounter(secret, currentCounter + offset);
            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII))) {
                return true;
            }
        }
        return false;
    }

    static String codeForCounter(String base32Secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(base32Secret), "HmacSHA1"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format(java.util.Locale.ROOT, "%06d", otp);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate an authenticator code.", exception);
        }
    }

    private AppUser findUser(long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account was not found."));
    }

    private AppUser findUserForUpdate(long userId) {
        return appUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account was not found."));
    }

    private void requireCurrentPassword(AppUser user, String currentPassword) {
        if (currentPassword == null
                || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
    }

    private String encrypt(String value) {
        try {
            byte[] nonce = new byte[12];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                            .put(nonce)
                            .put(encrypted)
                            .array());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to protect the authenticator secret.", exception);
        }
    }

    private String decrypt(String encryptedValue) {
        try {
            byte[] value = Base64.getUrlDecoder().decode(encryptedValue);
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] nonce = new byte[12];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to decrypt the stored authenticator secret.", exception);
        }
    }

    private SecretKeySpec encryptionKey() throws Exception {
        byte[] jwtKey = Base64.getDecoder().decode(jwtSecret);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtKey, "HmacSHA256"));
        byte[] derivedKey = mac.doFinal(
                "erp-totp-secret-encryption-v1".getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(derivedKey, "AES");
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash the authentication challenge.", exception);
        }
    }

    private static String encodeBase32(byte[] bytes) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsInBuffer = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsInBuffer += 8;
            while (bitsInBuffer >= 5) {
                result.append(alphabet.charAt((buffer >> (bitsInBuffer - 5)) & 0x1f));
                bitsInBuffer -= 5;
            }
        }
        if (bitsInBuffer > 0) {
            result.append(alphabet.charAt((buffer << (5 - bitsInBuffer)) & 0x1f));
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String normalized = value.replace("=", "").toUpperCase(java.util.Locale.ROOT);
        ByteBuffer output = ByteBuffer.allocate(normalized.length() * 5 / 8);
        int buffer = 0;
        int bitsInBuffer = 0;
        for (char character : normalized.toCharArray()) {
            int index = alphabet.indexOf(character);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid authenticator secret.");
            }
            buffer = (buffer << 5) | index;
            bitsInBuffer += 5;
            if (bitsInBuffer >= 8) {
                output.put((byte) ((buffer >> (bitsInBuffer - 8)) & 0xff));
                bitsInBuffer -= 8;
            }
        }
        byte[] decoded = new byte[output.position()];
        output.flip();
        output.get(decoded);
        return decoded;
    }
}
