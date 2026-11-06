package com.sass.ramel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "two_factor_login_challenges")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TwoFactorLoginChallenge {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Column(name = "challenge_hash", nullable = false, unique = true, length = 64)
    private String challengeHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    public TwoFactorLoginChallenge(
            UUID id,
            AppUser user,
            String challengeHash,
            OffsetDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.challengeHash = challengeHash;
        this.expiresAt = expiresAt;
    }

    public void recordFailedAttempt(OffsetDateTime now, int maximumAttempts) {
        failedAttempts++;
        if (failedAttempts >= maximumAttempts) {
            consumedAt = now;
        }
    }

    public void consume(OffsetDateTime now) {
        consumedAt = now;
    }
}
