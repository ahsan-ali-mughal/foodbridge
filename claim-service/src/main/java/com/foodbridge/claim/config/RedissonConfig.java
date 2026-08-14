package com.foodbridge.claim.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single-node Redisson client configuration. For production, point
 * {@code redis.address} at a Redis Cluster or Sentinel topology and switch
 * to {@code Config#useClusterServers()} / {@code useSentinelServers()} —
 * the {@code RLock} API used by ClaimServiceImpl is unaffected either way.
 */
@Configuration
public class RedissonConfig {

    @Value("${redis.address}")
    private String redisAddress;

    @Value("${redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress(redisAddress)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4)
                .setTimeout(3000);
        if (!redisPassword.isBlank()) {
            serverConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
