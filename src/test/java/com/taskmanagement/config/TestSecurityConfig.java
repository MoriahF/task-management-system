package com.taskmanagement.config;

import com.taskmanagement.security.JwtTokenValidator;
import com.taskmanagement.security.SecurityContextHelper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityContextHelper securityContextHelper() {
        return mock(SecurityContextHelper.class);
    }

    @Bean
    @Primary
    public JwtTokenValidator jwtTokenValidator() {
        return mock(JwtTokenValidator.class);
    }
}