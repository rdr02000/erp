package com.ai.rag.controller;

import com.ai.rag.dto.UserApiModels.ApiError;
import com.ai.rag.dto.UserApiModels.CreateUserRequest;
import com.ai.rag.dto.UserApiModels.ProfileRequest;
import com.ai.rag.dto.UserApiModels.ProfileView;
import com.ai.rag.dto.UserApiModels.UpdateUserRequest;
import com.ai.rag.dto.UserApiModels.UserView;
import com.ai.rag.security.AuthenticatedUser;
import com.ai.rag.service.UserManagementService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/users")
    public List<UserView> getUsers(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return List.of(userManagementService.getUser(currentUser.id()));
    }

    @GetMapping("/users/{userId}")
    public UserView getUser(
            @PathVariable long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        return userManagementService.getUser(userId);
    }

    @PostMapping("/users")
    public ResponseEntity<UserView> createUser(@RequestBody CreateUserRequest request) {
        UserView user = userManagementService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
        return ResponseEntity.created(location).body(user);
    }

    @PutMapping("/users/{userId}")
    public UserView updateUser(
            @PathVariable long userId,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        return userManagementService.updateUser(userId, request);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profiles")
    public List<ProfileView> getProfiles(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userManagementService.getProfileIfPresent(currentUser.id())
                .map(List::of)
                .orElseGet(List::of);
    }

    @GetMapping("/users/{userId}/profile")
    public ProfileView getProfile(
            @PathVariable long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        return userManagementService.getProfile(userId);
    }

    @PostMapping("/users/{userId}/profile")
    public ResponseEntity<ProfileView> createProfile(
            @PathVariable long userId,
            @RequestBody ProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        ProfileView profile = userManagementService.createProfile(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(profile);
    }

    @PutMapping("/users/{userId}/profile")
    public ProfileView updateProfile(
            @PathVariable long userId,
            @RequestBody ProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        return userManagementService.updateProfile(userId, request);
    }

    @DeleteMapping("/users/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        assertOwnUser(userId, currentUser);
        userManagementService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Username or profile email already exists."));
    }

    private static void assertOwnUser(long userId, AuthenticatedUser currentUser) {
        if (currentUser.id() != userId) {
            throw new AccessDeniedException("You can only manage your own account.");
        }
    }
}
