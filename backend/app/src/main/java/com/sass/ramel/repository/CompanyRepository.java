package com.sass.ramel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sass.ramel.entity.Company;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	Optional<Company> findByNameIgnoreCase(String name);
}