package com.ai.rag.repository;

import com.ai.rag.entity.LoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findTop50ByUser_IdOrderByLoggedAtDesc(long userId);
}
