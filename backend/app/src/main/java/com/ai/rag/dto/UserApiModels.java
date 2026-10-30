package com.ai.rag.dto;

import java.time.OffsetDateTime;

public final class UserApiModels {

    private UserApiModels() {
    }

    public record CreateUserRequest(String username, String password) {
    }

        public record UpdateUserRequest(String username) {
    }

    public record UserView(long id, String username, OffsetDateTime createdAt) {
    }

    public record CompanyRequest(
            String name,
            String industry,
            String website,
            String email,
            String phone,
                        String address,
                        String logoUrl) {
                public CompanyRequest(
                                String name,
                                String industry,
                                String website,
                                String email,
                                String phone,
                                String address) {
                        this(name, industry, website, email, phone, address, null);
                }
    }

    public record CompanyView(
            long id,
            String name,
            String industry,
            String website,
            String email,
            String phone,
            String address,
            String logoUrl) {
    }

    public record ProfileRequest(
            String firstName,
            String lastName,
            String email,
            String phone,
            String profilePictureUrl,
            String address,
            String facebookUrl,
            String xUrl,
            String linkedinUrl,
                        String instagramUrl,
                        CompanyRequest company) {
                public ProfileRequest(
                                String firstName,
                                String lastName,
                                String email,
                                String phone,
                                String profilePictureUrl,
                                String address,
                                String facebookUrl,
                                String xUrl,
                                String linkedinUrl,
                                String instagramUrl) {
                        this(firstName, lastName, email, phone, profilePictureUrl, address,
                                        facebookUrl, xUrl, linkedinUrl, instagramUrl, null);
                }
    }

    public record ProfileView(
            long userId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String profilePictureUrl,
            String address,
            String facebookUrl,
            String xUrl,
            String linkedinUrl,
            String instagramUrl,
            CompanyView company,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record ApiError(String message) {
    }
}
