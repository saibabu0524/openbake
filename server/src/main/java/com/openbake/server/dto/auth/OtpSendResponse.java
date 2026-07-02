package com.openbake.server.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Mirrors backend/app/routers/auth.py's send_otp dev-mode response (no real SMS gateway wired up). */
public class OtpSendResponse {

    private final String message;

    @JsonProperty("dev_otp")
    private final String devOtp;

    public OtpSendResponse(String message, String devOtp) {
        this.message = message;
        this.devOtp = devOtp;
    }

    public String getMessage() { return message; }
    public String getDevOtp() { return devOtp; }
}
