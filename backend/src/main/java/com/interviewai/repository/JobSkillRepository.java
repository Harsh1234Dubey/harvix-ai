package com.interviewai.repository;

import com.interviewai.domain.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    List<JobSkill> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);
}
