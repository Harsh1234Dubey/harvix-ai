package com.interviewai.repository;

import com.interviewai.common.enums.ApplicationStatus;
import com.interviewai.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByUuid(UUID uuid);

    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findByJobId(Long jobId, Pageable pageable);

    List<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    long countByCandidateId(Long candidateId);

    @Query("select a.job.company.name as name, count(a) as total from Application a group by a.job.company.name order by total desc")
    List<Object[]> countGroupedByCompany();

    @Modifying
    @Query("update Application a set a.status = :status, a.updatedAt = current_timestamp where a.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ApplicationStatus status);
}
