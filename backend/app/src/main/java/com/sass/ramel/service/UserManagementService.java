package com.sass.ramel.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sass.ramel.dto.UserApiModels.CompanyRequest;
import com.sass.ramel.dto.UserApiModels.CompanyView;
import com.sass.ramel.dto.UserApiModels.CreateUserRequest;
import com.sass.ramel.dto.UserApiModels.ProfileRequest;
import com.sass.ramel.dto.UserApiModels.ProfileView;
import com.sass.ramel.dto.UserApiModels.UpdateUserRequest;
import com.sass.ramel.dto.UserApiModels.UserView;
import com.sass.ramel.entity.AppUser;
import com.sass.ramel.entity.Company;
import com.sass.ramel.entity.UserProfile;
import com.sass.ramel.repository.AppUserRepository;
import com.sass.ramel.repository.CompanyRepository;
import com.sass.ramel.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository appUserRepository;
    private final CompanyRepository companyRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserView> getUsers() {
        return appUserRepository.findAllByOrderByIdAsc()
                .stream()
                .map(UserManagementService::toUserView)
                .toList();
    }

    public UserView getUser(long userId) {
        return toUserView(findUser(userId));
    }

    @Transactional
    public UserView createUser(CreateUserRequest request) {
        if (request == null) {
            throw badRequest("User request body is required.");
        }
        String username = requiredText(request.username(), "Username", 255);
        String password = validatedPassword(request.password());
        AppUser user = new AppUser(username, passwordEncoder.encode(password));
        return toUserView(appUserRepository.save(user));
    }

    @Transactional
    public UserView updateUser(long userId, UpdateUserRequest request) {
        if (request == null) {
            throw badRequest("User request body is required.");
        }
        AppUser user = findUser(userId);
        user.setUsername(requiredText(request.username(), "Username", 255));
        return toUserView(appUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(long userId) {
        appUserRepository.delete(findUser(userId));
    }

    public List<ProfileView> getProfiles() {
        return userProfileRepository.findAllByOrderByUserIdAsc()
                .stream()
                .map(UserManagementService::toProfileView)
                .toList();
    }

    public ProfileView getProfile(long userId) {
        return toProfileView(findProfile(userId));
    }

    public Optional<ProfileView> getProfileIfPresent(long userId) {
        return userProfileRepository.findById(userId).map(UserManagementService::toProfileView);
    }

    @Transactional
    public ProfileView createProfile(long userId, ProfileRequest request) {
        validateProfile(request);
        AppUser user = findUser(userId);
        UserProfile profile = new UserProfile(user);
        applyProfile(profile, request);
        user.setProfile(profile);
        return toProfileView(userProfileRepository.save(profile));
    }

    @Transactional
    public ProfileView updateProfile(long userId, ProfileRequest request) {
        validateProfile(request);
        UserProfile profile = findProfile(userId);
        applyProfile(profile, request);
        return toProfileView(userProfileRepository.save(profile));
    }

    @Transactional
    public void deleteProfile(long userId) {
        userProfileRepository.delete(findProfile(userId));
    }

    private AppUser findUser(long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> notFound("User not found."));
    }

    private UserProfile findProfile(long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> notFound("User profile not found."));
    }

    private static void validateProfile(ProfileRequest request) {
        if (request == null) {
            throw badRequest("Profile request body is required.");
        }
        validateLength(request.firstName(), "First name", 100);
        validateLength(request.lastName(), "Last name", 100);
        validateLength(request.email(), "Email", 320);
        validateLength(request.phone(), "Phone", 32);
        validateLength(request.profilePictureUrl(), "Profile picture URL", 2048);
        validateLength(request.facebookUrl(), "Facebook URL", 2048);
        validateLength(request.xUrl(), "X URL", 2048);
        validateLength(request.linkedinUrl(), "LinkedIn URL", 2048);
        validateLength(request.instagramUrl(), "Instagram URL", 2048);
        CompanyRequest company = request.company();
        if (company != null) {
            boolean hasCompanyDetails = hasText(company.industry())
                    || hasText(company.website())
                    || hasText(company.email())
                    || hasText(company.phone())
                    || hasText(company.address())
                    || hasText(company.logoUrl());
            if (hasCompanyDetails && !hasText(company.name())) {
                throw badRequest("Company name is required when company details are provided.");
            }
            validateLength(company.name(), "Company name", 255);
            validateLength(company.industry(), "Company industry", 100);
            validateLength(company.website(), "Company website", 2048);
            validateLength(company.email(), "Company email", 320);
            validateLength(company.phone(), "Company phone", 32);
            validateLength(company.address(), "Company address", 10000);
            validateLength(company.logoUrl(), "Company logo URL", 2048);
        }
    }

    private void applyProfile(UserProfile profile, ProfileRequest request) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setAddress(request.address());
        profile.setFacebookUrl(request.facebookUrl());
        profile.setXUrl(request.xUrl());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setInstagramUrl(request.instagramUrl());
        if (request.company() != null) {
            profile.setCompany(resolveCompany(request.company()));
        }
    }

    private Company resolveCompany(CompanyRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) return null;

        Company company = companyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> new Company(name));
        company.setName(name);
        company.setIndustry(emptyToNull(request.industry()));
        company.setWebsite(emptyToNull(request.website()));
        company.setEmail(emptyToNull(request.email()));
        company.setPhone(emptyToNull(request.phone()));
        company.setAddress(emptyToNull(request.address()));
        company.setLogoUrl(emptyToNull(request.logoUrl()));
        return companyRepository.save(company);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String validatedPassword(String password) {
        if (password == null || password.isBlank()) {
            throw badRequest("Password is required.");
        }
        if (password.length() < 8) {
            throw badRequest("Password must contain at least 8 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest("Password must be at most 72 UTF-8 bytes.");
        }
        return password;
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required.");
        }
        String trimmed = value.trim();
        validateLength(trimmed, field, maxLength);
        return trimmed;
    }

    private static void validateLength(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw badRequest(field + " must be at most " + maxLength + " characters.");
        }
    }

    private static UserView toUserView(AppUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    private static ProfileView toProfileView(UserProfile profile) {
        return new ProfileView(
                profile.getUser().getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getProfilePictureUrl(),
                profile.getAddress(),
                profile.getFacebookUrl(),
                profile.getXUrl(),
                profile.getLinkedinUrl(),
                profile.getInstagramUrl(),
                toCompanyView(profile.getCompany()),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private static CompanyView toCompanyView(Company company) {
        if (company == null) return null;
        return new CompanyView(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getWebsite(),
                company.getEmail(),
                company.getPhone(),
                company.getAddress(),
                company.getLogoUrl());
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
