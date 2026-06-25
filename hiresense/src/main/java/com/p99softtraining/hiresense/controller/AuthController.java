package com.p99softtraining.hiresense.controller;

import com.p99softtraining.hiresense.dto.request.RegisterRequest;
import com.p99softtraining.hiresense.dto.response.UserResponse;
import com.p99softtraining.hiresense.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = userService.register(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }
}
