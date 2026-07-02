package com.openbake.server.security;

import com.openbake.server.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * HS256 JWT issuing/verification matching backend/app/utils/jwt.py claim-for-claim:
 * {sub, role, type: access|refresh, exp}.
 */
@Component
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey key;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.key = Keys.hmacShaKeyFor(appProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, String role) {
        return createToken(userId, role, "access", Duration.ofMinutes(appProperties.getAccessTokenExpireMinutes()));
    }

    public String createRefreshToken(String userId, String role) {
        return createToken(userId, role, "refresh", Duration.ofDays(appProperties.getRefreshTokenExpireDays()));
    }

    private String createToken(String userId, String role, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Parses and validates the signature/expiry only — does not check the "type" claim. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** Parses and validates that the token's "type" claim matches, mirroring verify_token(). */
    public Claims verify(String token, String expectedType) {
        Claims claims = parse(token);
        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }
}
