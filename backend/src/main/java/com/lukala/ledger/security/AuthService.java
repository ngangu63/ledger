package com.lukala.ledger.security;

import com.lukala.ledger.security.dto.LoginRequest;
import com.lukala.ledger.security.dto.TokenResponse;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Minimal authentication backed by seeded in-memory users. This is a development
 * stand-in — in production, back it with a user store / external IdP (e.g. AWS
 * Cognito) and remove the seeded credentials.
 */
@Service
public class AuthService {

    private record User(String passwordHash, List<String> roles) {
    }

    private final Map<String, User> users;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(PasswordEncoder encoder, JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.users = Map.of(
                "admin", new User(encoder.encode("admin123"), List.of("ADMIN")),
                "service", new User(encoder.encode("service123"), List.of("SERVICE")),
                "viewer", new User(encoder.encode("viewer123"), List.of("VIEWER")));
        this.encoder = encoder;
    }

    private final PasswordEncoder encoder;

    public TokenResponse login(LoginRequest request) {
        User user = users.get(request.username());
        if (user == null || !encoder.matches(request.password(), user.passwordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        String token = jwtService.issue(request.username(), user.roles());
        return new TokenResponse(token, "Bearer", jwtProperties.ttlSeconds(), user.roles());
    }
}
