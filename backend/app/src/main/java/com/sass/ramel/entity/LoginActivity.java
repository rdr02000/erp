package com.sass.ramel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "login_activities",
        indexes = @Index(
                name = "idx_login_activities_user_logged_at",
                columnList = "user_id, logged_at"))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "device_type", nullable = false, length = 32)
    private String deviceType;

    @Column(nullable = false, length = 64)
    private String browser;

    @Column(name = "operating_system", nullable = false, length = 64)
    private String operatingSystem;

    @Column(name = "user_agent", length = 2048)
    private String userAgent;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private OffsetDateTime loggedAt;

    public LoginActivity(
            AppUser user,
            String deviceType,
            String browser,
            String operatingSystem,
            String userAgent,
            Double latitude,
            Double longitude,
            Double accuracyMeters) {
        this.user = user;
        this.deviceType = deviceType;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.userAgent = userAgent;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
    }

    @PrePersist
    void onCreate() {
        if (loggedAt == null) {
            loggedAt = OffsetDateTime.now();
        }
    }
}
