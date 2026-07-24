package com.lukala.ledger.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.security.jwt")
public record JwtProperties(String secret, long ttlSeconds, String issuer) {
}
