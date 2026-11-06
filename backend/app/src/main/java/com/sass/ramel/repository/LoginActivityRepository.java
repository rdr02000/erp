package com.sass.ramel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sass.ramel.entity.LoginActivity;

import java.util.List;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findTop50ByUser_IdOrderByLoggedAtDesc(long userId);
}
