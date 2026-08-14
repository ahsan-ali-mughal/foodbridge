package com.foodbridge.auth.service.impl;

import com.foodbridge.auth.dto.AuthResponse;
import com.foodbridge.auth.dto.LoginRequest;
import com.foodbridge.auth.dto.RegisterRequest;
import com.foodbridge.auth.dto.UserResponse;
import com.foodbridge.auth.dto.VerifyNgoRequest;
import com.foodbridge.auth.entity.User;
import com.foodbridge.auth.repository.UserRepository;
import com.foodbridge.auth.security.JwtService;
import com.foodbridge.auth.service.AuthService;
import com.foodbridge.common.enums.Role;
import com.foodbridge.common.enums.VerificationStatus;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.EventType;
import com.foodbridge.common.event.UserRegisteredEvent;
import com.foodbridge.common.exception.ConflictException;
import com.foodbridge.common.exception.ResourceNotFoundException;
import com.foodbridge.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String TOPIC_USER_REGISTERED = "user.registered";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaTemplate<String, EventEnvelope<UserRegisteredEvent>> kafkaTemplate;

    public AuthServiceImpl(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            KafkaTemplate<String, EventEnvelope<UserRegisteredEvent>> kafkaTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.info("Processing registration request for email={} role={}", normalizedEmail, request.role());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Registration rejected: email already in use, email={}", normalizedEmail);
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
        }

        User user = User.newUser(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.role());
        User saved = userRepository.save(user);

        publishUserRegistered(saved);

        log.info("Registered new user id={} role={} verificationStatus={}",
                saved.getId(), saved.getRole(), saved.getVerificationStatus());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: no account for email={}", normalizedEmail);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!user.isEnabled()) {
            log.warn("Login rejected for disabled account, userId={}", user.getId());
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: bad credentials for userId={}", user.getId());
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("Login successful for userId={} role={}", user.getId(), user.getRole());
        return issueTokenPair(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new UnauthorizedException("Provided token is not a refresh token");
        }

        Long userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        log.info("Refreshed access token for userId={}", userId);
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public UserResponse verifyNgo(Long userId, VerifyNgoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRole() != Role.NGO) {
            throw new ConflictException("NOT_AN_NGO", "Only NGO accounts require verification");
        }

        user.setVerificationStatus(request.status());
        User saved = userRepository.save(user);
        log.info("Admin updated NGO verification, userId={} newStatus={}", userId, request.status());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listByRoleAndStatus(Role role, VerificationStatus status) {
        return userRepository.findByRoleAndVerificationStatus(role, status, Pageable.unpaged())
                .map(this::toResponse)
                .getContent();
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.isVerified());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return AuthResponse.bearer(accessToken, refreshToken, jwtService.accessTokenTtlSeconds());
    }

    private void publishUserRegistered(User user) {
        String correlationId = MDC.get("correlationId");
        var payload = new UserRegisteredEvent(user.getId(), user.getEmail(), user.getRole());
        var envelope = EventEnvelope.of(EventType.USER_REGISTERED, correlationId, payload);
        kafkaTemplate.send(TOPIC_USER_REGISTERED, user.getId().toString(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Publishing failure must never fail the registration transaction itself;
                        // it is logged loudly for alerting/reconciliation instead.
                        log.error("Failed to publish user.registered event for userId={}", user.getId(), ex);
                    } else {
                        log.debug("Published user.registered event for userId={}", user.getId());
                    }
                });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getVerificationStatus(), user.getCreatedAt());
    }
}
