package com.openbake.server.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store — mirrors the dev-mode _otp_store dict in backend/app/routers/auth.py.
 * TODO: back this with Redis + a real SMS gateway (Twilio/MSG91) for production.
 */
@Component
public class OtpStore {

    public record Entry(String otp, LocalDateTime expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void put(String phone, String otp, LocalDateTime expiresAt) {
        store.put(phone, new Entry(otp, expiresAt));
    }

    public Entry get(String phone) {
        return store.get(phone);
    }

    public void remove(String phone) {
        store.remove(phone);
    }
}
