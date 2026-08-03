package com.interviewai.repository;

import com.interviewai.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByUuid(UUID uuid);
}
