package com.openbake.server.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {
    private String phone;
    private String otp;
    private String name;
}
