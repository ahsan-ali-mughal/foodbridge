package com.foodbridge.common.event;

import com.foodbridge.common.enums.Role;

/**
 * Published by auth-service whenever a new account is created.
 * Consumed by notification-service (welcome email) and admin-service (analytics).
 *
 * @param userId id of the newly created user
 * @param email  registered email address
 * @param role   role the account was registered with
 */
public record UserRegisteredEvent(Long userId, String email, Role role) {
}
