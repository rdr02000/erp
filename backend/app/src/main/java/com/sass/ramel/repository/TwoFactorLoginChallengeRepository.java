package com.sass.ramel.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sass.ramel.entity.TwoFactorLoginChallenge;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TwoFactorLoginChallengeRepository
        extends JpaRepository<TwoFactorLoginChallenge, UUID> {

    long countByUser_IdAndExpiresAtAfter(long userId, OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from TwoFactorLoginChallenge challenge
            where challenge.challengeHash = :challengeHash
            """)
    Optional<TwoFactorLoginChallenge> findByChallengeHashForUpdate(
            @Param("challengeHash") String challengeHash);

    @Modifying
    @Transactional
    @Query("delete from TwoFactorLoginChallenge challenge where challenge.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") OffsetDateTime now);
}
