package com.ai.rag.repository;

import com.ai.rag.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findAllByOrderByUserIdAsc();
}
