package com.lukala.ledger.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LedgerPostingIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired LedgerService ledgerService;

    private Account newAccount(AccountType type) {
        return accountService.create(new CreateAccountRequest("acct-" + UUID.randomUUID(), type, "USD"));
    }

    private CreateTransactionRequest transfer(UUID debit, UUID credit, String amount, String ref) {
        return new CreateTransactionRequest("USD", ref, "test transfer", List.of(
                new PostingRequest(debit, PostingDirection.DEBIT, new BigDecimal(amount)),
                new PostingRequest(credit, PostingDirection.CREDIT, new BigDecimal(amount))));
    }

    @Test
    void balancedPostingUpdatesBothBalances() {
        Account cash = newAccount(AccountType.ASSET);
        Account revenue = newAccount(AccountType.REVENUE);

        ledgerService.post(transfer(cash.getId(), revenue.getId(), "100.00", null));

        // Debit-positive convention: asset debited is +100, revenue credited is -100 signed.
        assertThat(ledgerService.getBalance(cash.getId()).signedBalance())
                .isEqualByComparingTo("100.00");
        assertThat(ledgerService.getBalance(revenue.getId()).signedBalance())
                .isEqualByComparingTo("-100.00");
        // Natural balance reads positive for both a healthy asset and revenue.
        assertThat(ledgerService.getBalance(revenue.getId()).naturalBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void reversalRestoresBalances() {
        Account cash = newAccount(AccountType.ASSET);
        Account revenue = newAccount(AccountType.REVENUE);
        UUID txId = ledgerService.post(transfer(cash.getId(), revenue.getId(), "40.00", null));

        ledgerService.reverse(txId, "oops");

        assertThat(ledgerService.getBalance(cash.getId()).signedBalance()).isEqualByComparingTo("0.00");
        assertThat(ledgerService.getBalance(revenue.getId()).signedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void duplicateExternalRefIsRejected() {
        Account a = newAccount(AccountType.ASSET);
        Account b = newAccount(AccountType.REVENUE);
        String ref = "ext-" + UUID.randomUUID();
        ledgerService.post(transfer(a.getId(), b.getId(), "5.00", ref));

        assertThatThrownBy(() -> ledgerService.post(transfer(a.getId(), b.getId(), "5.00", ref)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void doubleReversalIsRejected() {
        Account a = newAccount(AccountType.ASSET);
        Account b = newAccount(AccountType.REVENUE);
        UUID txId = ledgerService.post(transfer(a.getId(), b.getId(), "5.00", null));
        ledgerService.reverse(txId, null);

        assertThatThrownBy(() -> ledgerService.reverse(txId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already reversed");
    }

    @Test
    void balanceOfUnknownAccountIsNotFound() {
        assertThatThrownBy(() -> ledgerService.getBalance(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }
}
