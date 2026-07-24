package com.lukala.ledger.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.common.exception.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdempotencyIT extends AbstractPostgresIT {

    @Autowired IdempotencyService idempotencyService;

    @Test
    void sameKeyAndBodyReturnsOriginalWithoutRerunningAction() {
        String key = "key-" + UUID.randomUUID();
        UUID first = idempotencyService.execute(key, "scope", "body-A", UUID::randomUUID);

        // A retry supplies a different result, but the recorded id must come back.
        UUID second = idempotencyService.execute(key, "scope", "body-A", UUID::randomUUID);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void sameKeyDifferentBodyIsRejected() {
        String key = "key-" + UUID.randomUUID();
        idempotencyService.execute(key, "scope", "body-A", UUID::randomUUID);

        assertThatThrownBy(() -> idempotencyService.execute(key, "scope", "body-B", UUID::randomUUID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already used for a different request");
    }

    @Test
    void nullKeyAlwaysRunsAction() {
        UUID a = idempotencyService.execute(null, "scope", "body", UUID::randomUUID);
        UUID b = idempotencyService.execute(null, "scope", "body", UUID::randomUUID);
        assertThat(a).isNotEqualTo(b);
    }
}
