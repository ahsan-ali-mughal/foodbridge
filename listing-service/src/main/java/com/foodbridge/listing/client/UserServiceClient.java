package com.foodbridge.listing.client;

import com.foodbridge.listing.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Synchronous call into auth-service to resolve a donor's profile before
 * accepting a listing on their behalf. This is a genuine "needs an answer
 * now" case (see inter-service communication guidance), unlike the
 * fire-and-forget Kafka events used elsewhere.
 */
@FeignClient(name = "auth-service", path = "/api/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserProfileResponse getUser(@PathVariable("userId") Long userId);
}
