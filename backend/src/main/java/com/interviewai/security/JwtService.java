package com.interviewai.security;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtl;
    private final long refreshTtl;
    private final long rememberMeTtl;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.access-token-expiration-ms}") long accessTtl,
                      @Value("${app.security.jwt.refresh-token-expiration-ms}") long refreshTtl,
                      @Value("${app.security.jwt.remember-me-expiration-ms}") long rememberMeTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.rememberMeTtl = rememberMeTtl;
    }

    public String generateAccessToken(User user) {
        return generate(user, AppConstants.TOKEN_TYPE_ACCESS, accessTtl);
    }

    public String generateRefreshToken(User user, boolean rememberMe) {
        return generate(user, AppConstants.TOKEN_TYPE_REFRESH, rememberMe ? rememberMeTtl : refreshTtl);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parse(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parse(token).get(AppConstants.TOKEN_CLAIM_ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public String extractType(String token) {
        return parse(token).get(AppConstants.TOKEN_CLAIM_TYPE, String.class);
    }

    public String extractUid(String token) {
        return parse(token).get("uid", String.class);
    }

    private String generate(User user, String type, long ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(AppConstants.TOKEN_CLAIM_TYPE, type)
                .claim(AppConstants.TOKEN_CLAIM_ROLES, user.getRoles().stream().map(r -> r.getCode()).toList())
                .claim("uid", user.getUuid().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttl)))
                .signWith(key)
                .compact();
    }
}
