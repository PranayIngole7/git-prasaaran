package com.pranay.gitprasaaran.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class SecurityController {

    @GetMapping
    public CurrentUserResponse currentUser(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();

        return new CurrentUserResponse(
                authentication.getName(),
                roles);
    }

    public record CurrentUserResponse(
            String email,
            List<String> roles) {
    }
}