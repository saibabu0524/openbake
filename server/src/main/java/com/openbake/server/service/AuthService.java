package com.openbake.server.service;

import com.openbake.server.config.AppProperties;
import com.openbake.server.dto.auth.RegisterRequest;
import com.openbake.server.dto.auth.TokenResponse;
import com.openbake.server.entity.RefreshToken;
import com.openbake.server.entity.User;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.RefreshTokenRepository;
import com.openbake.server.repository.UserRepository;
import com.openbake.server.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/** Mirrors backend/app/services/auth_service.py + the token-storage helpers in routers/auth.py. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties appProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public User registerUser(RegisterRequest data) {
        if (userRepository.findByEmail(data.getEmail()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        if (data.getPhone() != null && !data.getPhone().isBlank()
                && userRepository.findByPhone(data.getPhone()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number already registered");
        }
        User user = new User();
        user.setName(data.getName());
        user.setEmail(data.getEmail());
        user.setPhone(data.getPhone());
        user.setPasswordHash(passwordEncoder.encode(data.getPassword()));
        user.setAuthProvider("email");
        user.setRole("customer");
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return user;
    }

    public TokenResponse createTokens(User user) {
        return new TokenResponse(
                jwtService.createAccessToken(user.getId(), user.getRole()),
                jwtService.createRefreshToken(user.getId(), user.getRole())
        );
    }

    @Transactional
    public void storeRefreshToken(String userId, String rawToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusDays(appProperties.getRefreshTokenExpireDays()));
        refreshTokenRepository.save(token);
    }

    @Transactional
    public boolean revokeRefreshToken(String rawToken) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .map(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public TokenResponse rotateRefreshToken(String rawRefreshToken) {
        Claims claims = jwtService.verify(rawRefreshToken, "refresh");

        RefreshToken tokenRow = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked or is invalid"));

        if (tokenRow.getExpiresAt() != null && tokenRow.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRow.setRevoked(true);
            refreshTokenRepository.save(tokenRow);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        tokenRow.setRevoked(true);
        refreshTokenRepository.save(tokenRow);

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "customer";
        }

        String newAccess = jwtService.createAccessToken(userId, role);
        String newRefresh = jwtService.createRefreshToken(userId, role);
        storeRefreshToken(userId, newRefresh);

        return new TokenResponse(newAccess, newRefresh);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
