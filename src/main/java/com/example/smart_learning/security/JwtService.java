package com.example.smart_learning.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-ttl:PT2H}") Duration accessTtl) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTtl = accessTtl;
    }

    public String createAccessToken(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(Map.of(
                        "account", user.getUsername(),
                        "pv", user.getPermissions().privVersion(),
                        "type", "access",
                        "jti", UUID.randomUUID().toString()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public AccessTokenPayload parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                throw new JwtException("Not an access token");
            }
            return new AccessTokenPayload(
                    Long.valueOf(claims.getSubject()),
                    claims.get("account", String.class),
                    claims.get("pv", Number.class) == null ? 0L : claims.get("pv", Number.class).longValue(),
                    claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid access token", ex);
        }
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public record AccessTokenPayload(Long userId, String account, long privVersion, String jti) {
    }
}
