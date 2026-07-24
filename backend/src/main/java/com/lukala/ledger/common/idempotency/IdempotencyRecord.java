package com.lukala.ledger.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Records the outcome of an idempotent write so a retried request with the same
 * {@code Idempotency-Key} returns the original result instead of double-posting.
 * The {@code requestHash} guards against a key being reused for a different body.
 */
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String key;

    @Column(name = "scope", nullable = false, length = 64)
    private String scope;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    /** Id of the resource created by the original request (e.g. transaction id). */
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
        // JPA
    }

    public IdempotencyRecord(String key, String scope, String requestHash, UUID resourceId) {
        this.key = key;
        this.scope = scope;
        this.requestHash = requestHash;
        this.resourceId = resourceId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getScope() {
        return scope;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
