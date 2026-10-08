package com.pranay.gitprasaaran.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> {
                                })
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                "/api/v1/webhooks/**"))
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(
                                                                "/api/v1/health",
                                                                "/actuator/health",
                                                                "/api/v1/documents/**",
                                                                "/api/v1/webhooks/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/me").authenticated()
                                                .anyRequest().authenticated());

                return http.build();
        }
}