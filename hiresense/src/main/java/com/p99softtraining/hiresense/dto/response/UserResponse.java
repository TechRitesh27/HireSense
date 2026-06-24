package com.p99softtraining.hiresense.dto.response;

import com.p99softtraining.hiresense.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;

    private String fullName;

    private String email;

    private Role role;

}
