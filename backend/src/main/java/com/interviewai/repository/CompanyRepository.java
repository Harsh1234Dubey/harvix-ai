package com.interviewai.repository;

import com.interviewai.domain.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
