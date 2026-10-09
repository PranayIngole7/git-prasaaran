package com.pranay.gitprasaaran.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Development CORS so the Vite dev server can call the API.
 * This is not an authentication mechanism.
 */
@Configuration
public class DevCorsConfig implements WebMvcConfigurer {

    @Value("${gitprasaaran.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(
                        "GET",
                        "HEAD",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-GitHub-Delivery",
                        "X-Hub-Signature-256");
    }
}
