package com.ai.rag.service;

import com.ai.rag.dto.UserApiModels.CreateUserRequest;
import com.ai.rag.dto.UserApiModels.UpdateUserRequest;
import com.ai.rag.entity.AppUser;
import com.ai.rag.repository.AppUserRepository;
import com.ai.rag.repository.CompanyRepository;
import com.ai.rag.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserManagementServiceTest {

    private AppUserRepository appUserRepository;
    private UserManagementService service;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserManagementService(
            appUserRepository,
            companyRepository,
            userProfileRepository,
            passwordEncoder);
    }

    @Test
    void createUserHashesPasswordAndReturnsSafeView() {
        when(passwordEncoder.encode("correct-horse")).thenReturn("bcrypt-hash");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            return new AppUser("sample-user", "bcrypt-hash") {
                @Override
                public Long getId() {
                    return 42L;
                }

                @Override
                public OffsetDateTime getCreatedAt() {
                    return OffsetDateTime.parse("2026-01-01T00:00:00Z");
                }
            };
        });

        var result = service.createUser(new CreateUserRequest(" sample-user ", "correct-horse"));

        assertEquals(42L, result.id());
        assertEquals("sample-user", result.username());
        verify(passwordEncoder).encode("correct-horse");
        verify(appUserRepository).save(argThat(user ->
                user.getUsername().equals("sample-user") &&
                        user.getPasswordHash().equals("bcrypt-hash")));
    }

    @Test
    void createUserRejectsShortPasswordBeforeSaving() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.createUser(new CreateUserRequest("sample-user", "short")));

        verifyNoInteractions(passwordEncoder);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateUserWithoutPasswordKeepsExistingHash() {
        AppUser user = new AppUser("old-name", "existing-hash") {
            @Override
            public Long getId() {
                return 7L;
            }

            @Override
            public OffsetDateTime getCreatedAt() {
                return OffsetDateTime.parse("2026-01-01T00:00:00Z");
            }
        };
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        var result = service.updateUser(7L, new UpdateUserRequest("new-name"));

        assertEquals("new-name", result.username());
        assertEquals("existing-hash", user.getPasswordHash());
        verifyNoInteractions(passwordEncoder);
    }
}
