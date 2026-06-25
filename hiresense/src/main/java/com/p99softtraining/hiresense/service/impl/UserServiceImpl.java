package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.dto.request.CreateUserRequest;
import com.p99softtraining.hiresense.dto.request.LoginRequest;
import com.p99softtraining.hiresense.dto.request.RegisterRequest;
import com.p99softtraining.hiresense.dto.response.AuthResponse;
import com.p99softtraining.hiresense.dto.response.UserResponse;
import com.p99softtraining.hiresense.entity.User;
import com.p99softtraining.hiresense.enums.Role;
import com.p99softtraining.hiresense.exception.ResourceAlreadyExistsException;
import com.p99softtraining.hiresense.exception.ResourceNotFoundException;
import com.p99softtraining.hiresense.repository.UserRepository;
import com.p99softtraining.hiresense.security.JwtService;
import com.p99softtraining.hiresense.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (request.getRole() != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Only super admin can be registered from this endpoint");
        }

        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            throw new ResourceAlreadyExistsException("Super admin already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(Role.SUPER_ADMIN);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getEmail());

        return buildAuthResponse(token, savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() ->
                            new BadCredentialsException("Invalid credentials")
                        );

        boolean passwordMatches = passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return buildAuthResponse(token, user);
    }

    @Override
    public UserResponse createInterviewer(CreateUserRequest request) {

        User companyAdmin = getCurrentUser();

        if (companyAdmin.getCompany() == null) {
            throw new IllegalArgumentException("Company admin is not assigned to a company");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        User interviewer = new User();
        interviewer.setFullName(request.getFullName());
        interviewer.setEmail(request.getEmail());
        interviewer.setPassword(passwordEncoder.encode(request.getPassword()));
        interviewer.setRole(Role.INTERVIEWER);
        interviewer.setCompany(companyAdmin.getCompany());

        return mapUser(userRepository.save(interviewer));
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private AuthResponse buildAuthResponse(String token, User user) {

        return AuthResponse.builder()
                .token(token)
                .user(mapUser(user))
                .build();
    }

    private UserResponse mapUser(User user) {

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole());

        if (user.getCompany() != null) {
            builder.companyId(user.getCompany().getId());
            builder.companyName(user.getCompany().getName());
        }

        return builder.build();
    }
}
