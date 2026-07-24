package com.lukala.ledger.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the pessimistic-lock path in {@link LedgerService#post} does not lose
 * updates: many concurrent postings to the same account must sum exactly.
 */
class ConcurrencyIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired LedgerService ledgerService;

    @Test
    void concurrentPostingsToSameAccountDoNotLoseUpdates() throws InterruptedException {
        Account source = accountService.create(new CreateAccountRequest("src", AccountType.EQUITY, "USD"));
        Account target = accountService.create(new CreateAccountRequest("dst", AccountType.ASSET, "USD"));

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    ledgerService.post(new CreateTransactionRequest("USD", null, "concurrent", List.of(
                            new PostingRequest(target.getId(), PostingDirection.DEBIT, new BigDecimal("10.00")),
                            new PostingRequest(source.getId(), PostingDirection.CREDIT, new BigDecimal("10.00")))));
                    succeeded.incrementAndGet();
                } catch (Exception ignored) {
                    // Retryable contention failures would surface here; none expected
                    // because the pessimistic lock serializes writers.
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(succeeded.get()).isEqualTo(threads);
        assertThat(ledgerService.getBalance(target.getId()).signedBalance())
                .isEqualByComparingTo(new BigDecimal("10.00").multiply(BigDecimal.valueOf(threads)));
    }
}
