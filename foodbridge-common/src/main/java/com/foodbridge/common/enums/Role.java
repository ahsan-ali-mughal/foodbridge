package com.foodbridge.common.enums;

/**
 * Platform-wide user roles used for authentication and authorization decisions.
 * Kept in the shared module so every service applies the exact same role set.
 */
public enum Role {
    DONOR,
    NGO,
    VOLUNTEER,
    ADMIN
}
