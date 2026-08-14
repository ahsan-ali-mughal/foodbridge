package com.foodbridge.auth.entity;

import com.foodbridge.common.enums.Role;
import com.foodbridge.common.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Platform user account. A single table serves all four roles ({@link Role});
 * role-specific attributes are intentionally kept minimal here and would be
 * expanded in a dedicated profile service if/when the domain grows.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Column(nullable = false)
    private boolean enabled;

    public static User newUser(String email, String passwordHash, String displayName, Role role) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.role = role;
        // Donors and Volunteers self-verify; NGOs require Admin verification before claiming.
        user.verificationStatus = role == Role.NGO ? VerificationStatus.PENDING : VerificationStatus.VERIFIED;
        user.enabled = true;
        user.createdAt = Instant.now();
        return user;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }
}
