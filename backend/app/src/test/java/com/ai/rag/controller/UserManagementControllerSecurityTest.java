package com.ai.rag.controller;

import com.ai.rag.security.AuthenticatedUser;
import com.ai.rag.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class UserManagementControllerSecurityTest {

    private final UserManagementService userManagementService =
            mock(UserManagementService.class);
    private final UserManagementController controller =
            new UserManagementController(userManagementService);
    private final AuthenticatedUser currentUser =
            new AuthenticatedUser(11, "current-user", "current@example.test");

    @Test
    void rejectsAccessToAnotherUsersAccount() {
        assertThrows(
                AccessDeniedException.class,
                () -> controller.getUser(12, currentUser));
        verifyNoInteractions(userManagementService);
    }

    @Test
    void rejectsAccessToAnotherUsersProfile() {
        assertThrows(
                AccessDeniedException.class,
                () -> controller.getProfile(12, currentUser));
        verifyNoInteractions(userManagementService);
    }
}
