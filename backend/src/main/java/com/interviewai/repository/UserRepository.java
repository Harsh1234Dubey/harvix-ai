package com.interviewai.repository;

import com.interviewai.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    @Modifying
    @Query("update User u set u.lastLoginAt = :now, u.lastLoginIp = :ip where u.id = :id")
    void markLoggedIn(@Param("id") Long id, @Param("now") Instant now, @Param("ip") String ip);

    @Modifying
    @Query("update User u set u.emailVerified = true, u.status = 'ACTIVE' where u.id = :id")
    void markEmailVerified(@Param("id") Long id);

    @Modifying
    @Query("update User u set u.passwordHash = :hash where u.id = :id")
    void updatePassword(@Param("id") Long id, @Param("hash") String hash);
}
