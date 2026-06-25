package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.LoginRequest;
import com.p99softtraining.hiresense.dto.request.CreateUserRequest;
import com.p99softtraining.hiresense.dto.request.RegisterRequest;
import com.p99softtraining.hiresense.dto.response.AuthResponse;
import com.p99softtraining.hiresense.dto.response.UserResponse;

public interface UserService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest loginRequest);

    UserResponse createInterviewer(CreateUserRequest request);
}
