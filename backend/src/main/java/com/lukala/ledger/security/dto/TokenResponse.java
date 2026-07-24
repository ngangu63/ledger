package com.lukala.ledger.security.dto;

import java.util.List;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds,
                            List<String> roles) {
}
