package com.openbake.server.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSendRequest {
    private String phone;
}
