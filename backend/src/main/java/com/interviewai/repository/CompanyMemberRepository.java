package com.interviewai.repository;

import com.interviewai.domain.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {
    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
    Optional<CompanyMember> findByCompanyIdAndUserId(Long companyId, Long userId);
    List<CompanyMember> findByCompanyId(Long companyId);
    List<CompanyMember> findByUserId(Long userId);
}
