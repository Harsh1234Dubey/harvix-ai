package com.interviewai.repository;

import com.interviewai.common.enums.JobStatus;
import com.interviewai.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByUuid(UUID uuid);

    boolean existsByCompanyIdAndSlug(Long companyId, String slug);

    Page<Job> findByCompanyId(Long companyId, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    @Query("select j from Job j where j.status = 'PUBLISHED' and " +
            "(lower(j.title) like lower(concat('%', :q, '%')) " +
            "or lower(j.description) like lower(concat('%', :q, '%')))")
    Page<Job> search(@Param("q") String q, Pageable pageable);
}
