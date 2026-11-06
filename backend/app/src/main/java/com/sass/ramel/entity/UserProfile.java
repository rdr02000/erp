package com.sass.ramel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "first_name", length = 100)
    @Setter
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Setter
    private String lastName;

    @Column(unique = true, length = 320)
    @Setter
    private String email;

    @Column(length = 32)
    @Setter
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Setter
    private Company company;

    @Column(name = "profile_picture_url", length = 2048)
    @Setter
    private String profilePictureUrl;

    @Column(name = "facebook_url", length = 2048)
    @Setter
    private String facebookUrl;

    @Column(name = "x_url", length = 2048)
    @Setter
    private String xUrl;

    @Column(name = "linkedin_url", length = 2048)
    @Setter
    private String linkedinUrl;

    @Column(name = "instagram_url", length = 2048)
    @Setter
    private String instagramUrl;

    @Column(columnDefinition = "TEXT")
    @Setter
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserProfile(AppUser user) {
        setUser(user);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void setUser(AppUser user) {
        this.user = user;
    }
}
