package com.example.operation_atlas.config;


import com.example.operation_atlas.service.RateLimitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitService rateLimitService() {
        return new RateLimitService();
    }
}
