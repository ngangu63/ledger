package com.lukala.ledger.ledger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountRepository;
import com.lukala.ledger.account.AccountStatus;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.common.exception.UnbalancedTransactionException;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the validation branches of the posting engine that reject before
 * anything is written. Happy-path posting/reversal against a real schema lives in
 * {@link LedgerPostingIT}.
 */
@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock LedgerTransactionRepository transactionRepository;
    @Mock AccountBalanceRepository balanceRepository;

    private LedgerService service() {
        return new LedgerService(accountRepository, transactionRepository, balanceRepository);
    }

    private Account account(UUID id, String currency, AccountStatus status) {
        Account a = new Account(id, "acct", AccountType.ASSET, currency);
        a.setStatus(status);
        lenient().when(accountRepository.findById(id)).thenReturn(Optional.of(a));
        return a;
    }

    private static PostingRequest posting(UUID accountId, PostingDirection dir, String amount) {
        return new PostingRequest(accountId, dir, new BigDecimal(amount));
    }

    @Test
    void rejectsUnbalancedTransaction() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        account(a, "USD", AccountStatus.ACTIVE);
        account(b, "USD", AccountStatus.ACTIVE);
        var request = new CreateTransactionRequest("USD", null, "bad", List.of(
                posting(a, PostingDirection.DEBIT, "100.00"),
                posting(b, PostingDirection.DEBIT, "100.00")));

        assertThatThrownBy(() -> service().post(request))
                .isInstanceOf(UnbalancedTransactionException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsCurrencyMismatchBetweenAccountAndTransaction() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        account(a, "USD", AccountStatus.ACTIVE);
        account(b, "EUR", AccountStatus.ACTIVE);
        var request = new CreateTransactionRequest("USD", null, "mismatch", List.of(
                posting(a, PostingDirection.DEBIT, "10.00"),
                posting(b, PostingDirection.CREDIT, "10.00")));

        assertThatThrownBy(() -> service().post(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("but transaction is");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsPostingToFrozenAccount() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        account(a, "USD", AccountStatus.FROZEN);
        account(b, "USD", AccountStatus.ACTIVE);
        var request = new CreateTransactionRequest("USD", null, "frozen", List.of(
                posting(a, PostingDirection.DEBIT, "10.00"),
                posting(b, PostingDirection.CREDIT, "10.00")));

        assertThatThrownBy(() -> service().post(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be posted to");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateExternalRef() {
        when(transactionRepository.findByExternalRef("dup"))
                .thenReturn(Optional.of(new LedgerTransaction(UUID.randomUUID(), "dup", "existing")));
        var request = new CreateTransactionRequest("USD", "dup", "dup", List.of(
                posting(UUID.randomUUID(), PostingDirection.DEBIT, "10.00"),
                posting(UUID.randomUUID(), PostingDirection.CREDIT, "10.00")));

        assertThatThrownBy(() -> service().post(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }
}
