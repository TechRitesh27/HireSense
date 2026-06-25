package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;

    private UserResponse user;
}
