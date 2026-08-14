package com.foodbridge.auth.service;

import com.foodbridge.auth.dto.AuthResponse;
import com.foodbridge.auth.dto.LoginRequest;
import com.foodbridge.auth.dto.RegisterRequest;
import com.foodbridge.auth.dto.UserResponse;
import com.foodbridge.auth.dto.VerifyNgoRequest;
import com.foodbridge.common.enums.Role;
import com.foodbridge.common.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    UserResponse verifyNgo(Long userId, VerifyNgoRequest request);

    UserResponse getById(Long userId);

    List<UserResponse> listByRoleAndStatus(Role role, VerificationStatus status);
}
