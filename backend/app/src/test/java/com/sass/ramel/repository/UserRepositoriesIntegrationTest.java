package com.sass.ramel.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sass.ramel.config.PasswordConfiguration;
import com.sass.ramel.dto.AuthApiModels.LoginLocation;
import com.sass.ramel.dto.UserApiModels.CompanyRequest;
import com.sass.ramel.dto.UserApiModels.CreateUserRequest;
import com.sass.ramel.dto.UserApiModels.ProfileRequest;
import com.sass.ramel.dto.UserApiModels.UpdateUserRequest;
import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.Company;
import com.sass.ramel.entity.RefreshTokenSession;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.service.AuthService;
import com.sass.ramel.service.LoginActivityService;
import com.sass.ramel.service.UserManagementService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never"
})
@Import({
        AuthService.class,
        LoginActivityService.class,
        UserManagementService.class,
        PasswordConfiguration.class
})
class UserRepositoriesIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

        @Autowired
        private CompanyRepository companyRepository;

        @Autowired
        private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginActivityService loginActivityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void managesUserAndProfileThroughServiceWithPersistedHashedPassword() {
        var user = userManagementService.createUser(
                new CreateUserRequest("service-integration-user", "correct-horse"));

        assertEquals("service-integration-user", user.username());
        assertNotEquals("correct-horse", appUserRepository.findById(user.id()).orElseThrow()
                .getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "correct-horse",
                appUserRepository.findById(user.id()).orElseThrow().getPasswordHash()));

        var profile = userManagementService.createProfile(
                user.id(),
                new ProfileRequest("Avery", "Jordan", "avery@example.test",
                        "+1-555-0199", null, "456 Test Avenue",
                        "https://facebook.com/avery", "https://x.com/avery",
                        "https://linkedin.com/in/avery", "https://instagram.com/avery",
                        new CompanyRequest(
                                "Avery Systems",
                                "Technology",
                                "https://avery-systems.example",
                                "contact@avery-systems.example",
                                "+1-555-0198",
                                "456 Company Avenue",
                                "https://avery-systems.example/logo.png")));
        var persistedProfile = userManagementService.getProfile(user.id());
        assertEquals("Avery", persistedProfile.firstName());
        assertEquals("Avery Systems", persistedProfile.company().name());
        assertEquals("Technology", persistedProfile.company().industry());
        assertEquals("contact@avery-systems.example", persistedProfile.company().email());
        assertEquals(
                "https://avery-systems.example/logo.png",
                persistedProfile.company().logoUrl());
        assertEquals("https://facebook.com/avery", persistedProfile.facebookUrl());
        assertEquals("https://x.com/avery", persistedProfile.xUrl());
        assertEquals("https://linkedin.com/in/avery", persistedProfile.linkedinUrl());
        assertEquals("https://instagram.com/avery", persistedProfile.instagramUrl());

        var updatedUser = userManagementService.updateUser(
                user.id(),
                new UpdateUserRequest("updated-integration-user"));
        assertEquals("updated-integration-user", updatedUser.username());
        assertEquals("avery@example.test", profile.email());

        userManagementService.deleteUser(user.id());
        assertFalse(appUserRepository.existsById(user.id()));
        assertFalse(userProfileRepository.existsById(user.id()));
    }

    @Test
    void persistsUserProfileRelationshipAndCascadesUserDeletion() {
        AppUser user = appUserRepository.saveAndFlush(new AppUser("integration-user", "bcrypt-hash"));
                Company company = new Company("Example Company");
                company.setIndustry("Technology");
                company.setWebsite("https://example.test");
                company.setEmail("contact@example.test");
                company.setPhone("+1-555-0101");
                company.setAddress("789 Company Road");
                company = companyRepository.saveAndFlush(company);

        UserProfile profile = new UserProfile(user);
                profile.setCompany(company);
        profile.setFirstName("Taylor");
        profile.setLastName("Morgan");
        profile.setEmail("taylor.morgan@example.test");
        profile.setPhone("+1-555-0100");
        profile.setProfilePictureUrl("https://example.test/avatar.png");
        profile.setAddress("123 Sample Street");
        profile.setFacebookUrl("https://facebook.com/taylor");
        profile.setXUrl("https://x.com/taylor");
        profile.setLinkedinUrl("https://linkedin.com/in/taylor");
        profile.setInstagramUrl("https://instagram.com/taylor");
        user.setProfile(profile);
        appUserRepository.saveAndFlush(user);
        UUID refreshSessionId = UUID.randomUUID();
        refreshTokenSessionRepository.saveAndFlush(new RefreshTokenSession(
                refreshSessionId,
                appUserRepository.getReferenceById(user.getId()),
                "a".repeat(64),
                OffsetDateTime.now().plusDays(1)));

        assertEquals(user.getId(), appUserRepository
                .findByProfileEmailIgnoreCase("TAYLOR.MORGAN@example.test")
                .orElseThrow()
                .getId());

        UserProfile persistedProfile = userProfileRepository.findById(user.getId()).orElseThrow();
        assertEquals(user.getId(), persistedProfile.getUserId());
        assertEquals("Taylor", persistedProfile.getFirstName());
        assertEquals("taylor.morgan@example.test", persistedProfile.getEmail());
        assertEquals("Example Company", persistedProfile.getCompany().getName());
        assertEquals("Technology", persistedProfile.getCompany().getIndustry());
        assertEquals("https://example.test", persistedProfile.getCompany().getWebsite());
        assertEquals("https://facebook.com/taylor", persistedProfile.getFacebookUrl());
        assertEquals("https://x.com/taylor", persistedProfile.getXUrl());
        assertEquals("https://linkedin.com/in/taylor", persistedProfile.getLinkedinUrl());
        assertEquals("https://instagram.com/taylor", persistedProfile.getInstagramUrl());
        assertNotNull(persistedProfile.getCreatedAt());
        assertNotNull(persistedProfile.getUpdatedAt());

        appUserRepository.deleteById(user.getId());
        appUserRepository.flush();

        assertFalse(appUserRepository.existsById(user.getId()));
        assertFalse(userProfileRepository.existsById(user.getId()));
        assertFalse(refreshTokenSessionRepository.existsById(refreshSessionId));
        assertTrue(companyRepository.existsById(company.getId()));
    }

    @Test
    void authenticatesPersistedUserByProfileEmailAndBcryptPassword() {
        AppUser user = new AppUser(
                "login-integration-user",
                passwordEncoder.encode("integration-password"));
        UserProfile profile = new UserProfile(user);
        profile.setEmail("login.integration@example.test");
        user.setProfile(profile);
        appUserRepository.saveAndFlush(user);

        var authenticatedUser = authService.authenticate(
                "LOGIN.INTEGRATION@example.test",
                "integration-password");

        assertEquals(user.getId(), authenticatedUser.id());
        assertEquals("login.integration@example.test", authenticatedUser.email());
    }

    @Test
    void persistsUserAgentBrowserDeviceAndOptionalLocationForLogin() {
        AppUser user = appUserRepository.saveAndFlush(new AppUser(
                "activity-integration-user",
                passwordEncoder.encode("integration-password")));

        loginActivityService.recordSuccessfulLogin(
                user.getId(),
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
                new LoginLocation(37.7749, -122.4194, 25.0));

        var activity = loginActivityService.getRecentActivity(user.getId()).getFirst();
        assertEquals("Desktop", activity.deviceType());
        assertEquals("Chrome 129.0.0.0", activity.browser());
        assertEquals("Windows", activity.operatingSystem());
        assertEquals(37.7749, activity.latitude());
        assertEquals(-122.4194, activity.longitude());
        assertEquals(25.0, activity.accuracyMeters());
        assertNotNull(activity.loggedAt());
        assertTrue(loginActivityService.getRecentActivity(user.getId() + 1).isEmpty());
    }
}
