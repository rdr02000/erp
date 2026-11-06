package com.sass.ramel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.sass.ramel.dto.UserApiModels.CompanyRequest;
import com.sass.ramel.dto.UserApiModels.CreateUserRequest;
import com.sass.ramel.dto.UserApiModels.ProfileRequest;
import com.sass.ramel.dto.UserApiModels.UpdateUserRequest;
import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.Company;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.repository.CompanyRepository;
import com.sass.ramel.repository.UserProfileRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private static final long USER_ID = 42L;
    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService service;

    @Test
    void createUserTrimsUsernameHashesPasswordAndReturnsSafeView() {
        AppUser savedUser = user(USER_ID, "sample-user", "bcrypt-hash");
        when(passwordEncoder.encode("correct-horse")).thenReturn("bcrypt-hash");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);

        var result = service.createUser(
                new CreateUserRequest(" sample-user ", "correct-horse"));

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.username()).isEqualTo("sample-user");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        verify(passwordEncoder).encode("correct-horse");
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void createUserRejectsMissingRequest() {
        assertBadRequest(() -> service.createUser(null), "User request body is required.");

        verifyNoInteractions(passwordEncoder, appUserRepository);
    }

    @Test
    void createUserRejectsBlankUsername() {
        assertBadRequest(
                () -> service.createUser(new CreateUserRequest("  ", "correct-horse")),
                "Username is required.");

        verifyNoInteractions(passwordEncoder, appUserRepository);
    }

    @Test
    void createUserRejectsUsernameLongerThan255Characters() {
        String username = "u".repeat(256);

        assertBadRequest(
                () -> service.createUser(new CreateUserRequest(username, "correct-horse")),
                "Username must be at most 255 characters.");

        verifyNoInteractions(passwordEncoder, appUserRepository);
    }

    @Test
    void createUserRejectsShortPasswordBeforeSaving() {
        assertBadRequest(
                () -> service.createUser(new CreateUserRequest("sample-user", "short")),
                "Password must contain at least 8 characters.");

        verifyNoInteractions(passwordEncoder, appUserRepository);
    }

    @Test
    void createUserRejectsPasswordExceeding72Utf8Bytes() {
        String password = "é".repeat(37);

        assertBadRequest(
                () -> service.createUser(new CreateUserRequest("sample-user", password)),
                "Password must be at most 72 UTF-8 bytes.");

        verifyNoInteractions(passwordEncoder, appUserRepository);
    }

    @Test
    void getUsersMapsUsersInRepositoryOrderToSafeViews() {
        when(appUserRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(
                        user(1L, "first-user", "first-hash"),
                        user(2L, "second-user", "second-hash")));

        var result = service.getUsers();

        assertThat(result)
                .extracting(view -> view.id() + ":" + view.username())
                .containsExactly("1:first-user", "2:second-user");
    }

    @Test
    void getUserReturnsNotFoundWhenUserDoesNotExist() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.getUser(USER_ID));
    }

    @Test
    void updateUserTrimsUsernameAndPreservesPasswordHash() {
        AppUser existingUser = user(USER_ID, "old-name", "existing-hash");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(existingUser)).thenReturn(existingUser);

        var result = service.updateUser(USER_ID, new UpdateUserRequest(" new-name "));

        assertThat(result.username()).isEqualTo("new-name");
        assertThat(existingUser.getPasswordHash()).isEqualTo("existing-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserRejectsMissingRequestBeforeLoadingUser() {
        assertBadRequest(() -> service.updateUser(USER_ID, null), "User request body is required.");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    void updateUserReturnsNotFoundWhenUserDoesNotExist() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.updateUser(USER_ID, new UpdateUserRequest("new-name")));

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void deleteUserDeletesTheLoadedUser() {
        AppUser existingUser = user(USER_ID, "sample-user", "password-hash");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        service.deleteUser(USER_ID);

        verify(appUserRepository).delete(existingUser);
    }

    @Test
    void deleteUserReturnsNotFoundWhenUserDoesNotExist() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.deleteUser(USER_ID));

        verify(appUserRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void getProfileReturnsNotFoundWhenProfileDoesNotExist() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.getProfile(USER_ID));
    }

    @Test
    void getProfileIfPresentReturnsEmptyWhenProfileDoesNotExist() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getProfileIfPresent(USER_ID)).isEmpty();
    }

    @Test
    void createProfileSavesProfileAndAssociatesCompany() {
        AppUser existingUser = user(USER_ID, "sample-user", "password-hash");
        Company company = company(101L, "Acme");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(companyRepository.findByNameIgnoreCase("Acme")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company submittedCompany = invocation.getArgument(0);
            company.setIndustry(submittedCompany.getIndustry());
            company.setWebsite(submittedCompany.getWebsite());
            company.setEmail(submittedCompany.getEmail());
            company.setPhone(submittedCompany.getPhone());
            company.setAddress(submittedCompany.getAddress());
            company.setLogoUrl(submittedCompany.getLogoUrl());
            return company;
        });
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProfileRequest request = profileRequest(
                "Alex",
                "Rivera",
                "alex@example.test",
                new CompanyRequest(" Acme ", "Technology", null, null, null, null));

        var result = service.createProfile(USER_ID, request);

        assertThat(result.firstName()).isEqualTo("Alex");
        assertThat(result.email()).isEqualTo("alex@example.test");
        assertThat(result.company().name()).isEqualTo("Acme");
        assertThat(result.company().industry()).isEqualTo("Technology");
        assertThat(existingUser.getProfile()).isNotNull();
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void createProfileRejectsMissingRequest() {
        assertBadRequest(() -> service.createProfile(USER_ID, null), "Profile request body is required.");

        verifyNoInteractions(appUserRepository, userProfileRepository);
    }

    @Test
    void createProfileReturnsNotFoundWhenUserDoesNotExist() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.createProfile(
                USER_ID, profileRequest("Alex", null, null, null)));

        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void createProfileRejectsCompanyDetailsWithoutCompanyName() {
        ProfileRequest request = profileRequest(
                "Alex",
                null,
                null,
                new CompanyRequest(null, "Technology", null, null, null, null));

        assertBadRequest(
                () -> service.createProfile(USER_ID, request),
                "Company name is required when company details are provided.");

        verifyNoInteractions(appUserRepository, companyRepository);
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void updateProfileRejectsOversizedEmailBeforeLoadingProfile() {
        String email = "e".repeat(321);

        assertBadRequest(
                () -> service.updateProfile(
                        USER_ID, profileRequest("Alex", null, email, null)),
                "Email must be at most 320 characters.");

        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void updateProfileReplacesProfileFields() {
        AppUser owner = user(USER_ID, "sample-user", "password-hash");
        UserProfile profile = new UserProfile(owner);
        profile.setFirstName("Old");
        profile.setEmail("old@example.test");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        var result = service.updateProfile(
                USER_ID, profileRequest("New", "Name", "new@example.test", null));

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.lastName()).isEqualTo("Name");
        assertThat(result.email()).isEqualTo("new@example.test");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void deleteProfileDeletesTheLoadedProfile() {
        UserProfile profile = new UserProfile(new AppUser("sample-user", "password-hash"));
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));

        service.deleteProfile(USER_ID);

        verify(userProfileRepository).delete(profile);
    }

    private static AppUser user(long id, String username, String passwordHash) {
        return new AppUser(username, passwordHash) {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public OffsetDateTime getCreatedAt() {
                return CREATED_AT;
            }
        };
    }

    private static Company company(long id, String name) {
        return new Company(name) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    private static ProfileRequest profileRequest(
            String firstName,
            String lastName,
            String email,
            CompanyRequest company) {
        return new ProfileRequest(
                firstName, lastName, email, null, null, null,
                null, null, null, null, company);
    }

    private static void assertBadRequest(ThrowingCallable action, String expectedReason) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo(expectedReason);
                });
    }

    private static void assertNotFound(ThrowingCallable action) {
                assertThatThrownBy(action)
                        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
