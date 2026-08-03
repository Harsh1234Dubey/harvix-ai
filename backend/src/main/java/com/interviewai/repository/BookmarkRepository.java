package com.interviewai.repository;

import com.interviewai.domain.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    boolean existsByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    void deleteByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    Page<Bookmark> findByUserIdAndEntityType(Long userId, String entityType, Pageable pageable);
    long countByUserIdAndEntityType(Long userId, String entityType);
}
