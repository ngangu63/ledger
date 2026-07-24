package com.lukala.ledger.common.idempotency;

import com.lukala.ledger.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures write operations run at-most-once per {@code Idempotency-Key}.
 *
 * <p>On a first call the {@code action} runs and its resulting resource id is
 * recorded against the key. A retry with the same key and same request body
 * returns the recorded id without re-running the action; a retry with the same
 * key but a <em>different</em> body is rejected as a conflict.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * @param key         caller-supplied idempotency key (nullable — null means no
     *                    idempotency protection, the action just runs)
     * @param scope       logical operation name, so the same key can be reused
     *                    across unrelated endpoints
     * @param requestBody canonical request payload, hashed to detect body changes
     * @param action      the write to perform, returning the created resource id
     * @return the resource id, whether freshly created or previously recorded
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UUID execute(String key, String scope, String requestBody, Supplier<UUID> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        String hash = sha256(scope + '|' + requestBody);

        Optional<IdempotencyRecord> existing = repository.findById(key);
        if (existing.isPresent()) {
            return validateAndReturn(existing.get(), scope, hash);
        }

        UUID resourceId = action.get();
        try {
            repository.saveAndFlush(new IdempotencyRecord(key, scope, hash, resourceId));
        } catch (DataIntegrityViolationException raceLost) {
            // A concurrent in-flight request with the same key won the insert.
            // This transaction is now rollback-only, so we cannot recover its
            // result here — surface a conflict and let the client retry, which
            // will then hit the fast findById path above.
            throw new BusinessRuleException("IDEMPOTENCY_IN_PROGRESS",
                    "A concurrent request with the same Idempotency-Key is in progress; retry.");
        }
        return resourceId;
    }

    private UUID validateAndReturn(IdempotencyRecord record, String scope, String hash) {
        if (!record.getScope().equals(scope) || !record.getRequestHash().equals(hash)) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key already used for a different request.");
        }
        return record.getResourceId();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
