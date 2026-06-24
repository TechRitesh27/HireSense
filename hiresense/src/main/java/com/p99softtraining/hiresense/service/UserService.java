package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.RegisterRequest;
import com.p99softtraining.hiresense.dto.response.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);
}
