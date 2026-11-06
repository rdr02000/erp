package com.sass.ramel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sass.ramel.entity.UserProfile;

import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findAllByOrderByUserIdAsc();
}
