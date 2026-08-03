package com.interviewai.security;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.domain.Role;
import com.interviewai.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-256-xxxxx";
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15 * 60 * 1000L, 7 * 24 * 60 * 60 * 1000L, 30 * 24 * 60 * 60 * 1000L);
        Role admin = new Role();
        admin.setCode("ADMIN");
        Role candidate = new Role();
        candidate.setCode("CANDIDATE");
        user = new User();
        user.setId(1L);
        user.setUuid(UUID.randomUUID());
        user.setEmail("dev@interviewai.com");
        user.setRoles(Set.of(admin, candidate));
    }

    @Test
    void generatesAccessTokenWithClaims() {
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("dev@interviewai.com");
        assertThat(claims.get(AppConstants.TOKEN_CLAIM_TYPE)).isEqualTo(AppConstants.TOKEN_TYPE_ACCESS);
        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder("ADMIN", "CANDIDATE");
        assertThat(jwtService.extractUid(token)).isEqualTo(user.getUuid().toString());
    }

    @Test
    void generatesRefreshTokenWithLongerTtl() {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user, false);

        long accessTtl = jwtService.parse(access).getExpiration().getTime()
                - jwtService.parse(access).getIssuedAt().getTime();
        long refreshTtl = jwtService.parse(refresh).getExpiration().getTime()
                - jwtService.parse(refresh).getIssuedAt().getTime();

        assertThat(accessTtl).isLessThan(refreshTtl);
        assertThat(jwtService.extractType(refresh)).isEqualTo(AppConstants.TOKEN_TYPE_REFRESH);
    }

    @Test
    void isValidReturnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String[] parts = token.split("\\.");
        String payload = parts[1];
        String tamperedPayload = payload.substring(0, payload.length() - 3)
                + (payload.charAt(payload.length() - 3) == 'A' ? 'B' : 'A')
                + payload.substring(payload.length() - 2);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValidReturnsFalseForExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1000L, -1000L, -1000L);
        String token = shortLived.generateAccessToken(user);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void isValidReturnsTrueForValidToken() {
        assertThat(jwtService.isValid(jwtService.generateAccessToken(user))).isTrue();
    }
}
