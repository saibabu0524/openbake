package com.openbake.server.controller;

import com.openbake.server.dto.auth.*;
import com.openbake.server.entity.User;
import com.openbake.server.exception.ApiException;
import com.openbake.server.repository.UserRepository;
import com.openbake.server.security.FirebaseAuthService;
import com.openbake.server.service.AuthService;
import com.openbake.server.service.OtpStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/** 1:1 port of backend/app/routers/auth.py. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;
    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;
    private final OtpStore otpStore;

    public AuthController(AuthService authService, UserRepository userRepository,
                           FirebaseAuthService firebaseAuthService, OtpStore otpStore) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.firebaseAuthService = firebaseAuthService;
        this.otpStore = otpStore;
    }

    @PostMapping("/register")
    public TokenResponse register(@Valid @RequestBody RegisterRequest data) {
        User user = authService.registerUser(data);
        TokenResponse tokens = authService.createTokens(user);
        authService.storeRefreshToken(user.getId(), tokens.getRefreshToken());
        return tokens;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest data) {
        User user = authService.authenticate(data.getEmail(), data.getPassword());
        TokenResponse tokens = authService.createTokens(user);
        authService.storeRefreshToken(user.getId(), tokens.getRefreshToken());
        return tokens;
    }

    @PostMapping("/google")
    public TokenResponse googleAuth(@Valid @RequestBody GoogleAuthRequest data) {
        FirebaseAuthService.GoogleUser googleUser;
        try {
            googleUser = firebaseAuthService.verifyIdToken(data.getIdToken());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }

        User user = userRepository.findByEmail(googleUser.email()).orElseGet(() -> {
            User u = new User();
            u.setName(googleUser.name());
            u.setEmail(googleUser.email());
            u.setAuthProvider("google");
            u.setRole("customer");
            return userRepository.save(u);
        });

        TokenResponse tokens = authService.createTokens(user);
        authService.storeRefreshToken(user.getId(), tokens.getRefreshToken());
        return tokens;
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest data) {
        return authService.rotateRefreshToken(data.getRefreshToken());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest data) {
        authService.revokeRefreshToken(data.getRefreshToken());
        return new MessageResponse("Logged out successfully");
    }

    @PostMapping("/otp/send")
    public OtpSendResponse sendOtp(@RequestBody OtpSendRequest data) {
        String phone = data.getPhone() == null ? "" : data.getPhone().trim();
        if (phone.length() != 10 || !phone.chars().allMatch(Character::isDigit)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid 10-digit phone number");
        }
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        otpStore.put(phone, otp, LocalDateTime.now().plusMinutes(5));
        // TODO: send via SMS gateway (e.g. Twilio, MSG91) in production instead of returning dev_otp.
        return new OtpSendResponse("OTP sent to " + phone.substring(0, 3) + "****" + phone.substring(7), otp);
    }

    @PostMapping("/otp/verify")
    public TokenResponse verifyOtp(@RequestBody OtpVerifyRequest data) {
        String phone = data.getPhone() == null ? "" : data.getPhone().trim();
        String otp = data.getOtp() == null ? "" : data.getOtp().trim();
        String name = data.getName() == null ? "" : data.getName().trim();

        if (phone.isEmpty() || otp.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone and OTP are required");
        }

        OtpStore.Entry stored = otpStore.get(phone);
        if (stored == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No OTP was sent to this number. Please request a new one.");
        }
        if (stored.expiresAt() != null && LocalDateTime.now().isAfter(stored.expiresAt())) {
            otpStore.remove(phone);
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new one.");
        }
        if (!stored.otp().equals(otp)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }
        otpStore.remove(phone);

        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            if (name.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required for new users");
            }
            User u = new User();
            u.setName(name);
            u.setPhone(phone);
            u.setAuthProvider("phone");
            u.setRole("customer");
            return userRepository.save(u);
        });

        TokenResponse tokens = authService.createTokens(user);
        authService.storeRefreshToken(user.getId(), tokens.getRefreshToken());
        return tokens;
    }
}
