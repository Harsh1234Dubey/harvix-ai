package com.interviewai.repository;

import com.interviewai.domain.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Page<Certificate> findByCandidateId(Long candidateId, Pageable pageable);
    List<Certificate> findByCandidateId(Long candidateId);
    boolean existsByUuid(UUID uuid);
}
