package com.foodbridge.auth.repository;

import com.foodbridge.auth.entity.User;
import com.foodbridge.common.enums.Role;
import com.foodbridge.common.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRoleAndVerificationStatus(Role role, VerificationStatus status, Pageable pageable);
}
