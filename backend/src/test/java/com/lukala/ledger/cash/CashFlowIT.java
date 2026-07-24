package com.lukala.ledger.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.cash.dto.DepositRequest;
import com.lukala.ledger.cash.dto.WithdrawRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.ledger.LedgerTransaction;
import com.lukala.ledger.ledger.Posting;
import com.lukala.ledger.ledger.PostingDirection;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CashFlowIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired CashService cashService;
    @Autowired LedgerService ledgerService;

    private Account account(AccountType type, String currency) {
        return accountService.create(
                new CreateAccountRequest("acct-" + UUID.randomUUID(), type, currency));
    }

    @Test
    void depositIncreasesAssetBalanceWithBalancedPostings() {
        Account wallet = account(AccountType.ASSET, "USD");

        UUID txId = cashService.deposit(wallet.getId(), new DepositRequest(new BigDecimal("250.00"), null));

        assertThat(ledgerService.getBalance(wallet.getId()).naturalBalance())
                .isEqualByComparingTo("250.00");

        LedgerTransaction tx = ledgerService.getTransaction(txId);
        assertThat(tx.getPostings()).hasSize(2);
        // The wallet is debited (debit-normal asset grows) and the counter-leg credited.
        Posting walletLeg = tx.getPostings().stream()
                .filter(p -> p.getAccountId().equals(wallet.getId())).findFirst().orElseThrow();
        assertThat(walletLeg.getDirection()).isEqualTo(PostingDirection.DEBIT);
        // Debits equal credits: the two legs net to zero.
        BigDecimal net = tx.getPostings().stream()
                .map(p -> p.signedAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(net).isEqualByComparingTo("0.00");
    }

    @Test
    void depositAndWithdrawRoundTripToZero() {
        Account wallet = account(AccountType.ASSET, "USD");

        cashService.deposit(wallet.getId(), new DepositRequest(new BigDecimal("100.00"), null));
        cashService.withdraw(wallet.getId(), new WithdrawRequest(new BigDecimal("40.00"), null));

        assertThat(ledgerService.getBalance(wallet.getId()).naturalBalance())
                .isEqualByComparingTo("60.00");
    }

    @Test
    void withdrawBeyondBalanceIsRejected() {
        Account wallet = account(AccountType.ASSET, "USD");
        cashService.deposit(wallet.getId(), new DepositRequest(new BigDecimal("30.00"), null));

        assertThatThrownBy(() ->
                cashService.withdraw(wallet.getId(), new WithdrawRequest(new BigDecimal("50.00"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot withdraw");

        // The rejected withdrawal left the balance untouched.
        assertThat(ledgerService.getBalance(wallet.getId()).naturalBalance())
                .isEqualByComparingTo("30.00");
    }

    @Test
    void depositReusesOnePerCurrencyCashClearingAccount() {
        Account usdA = account(AccountType.ASSET, "USD");
        Account usdB = account(AccountType.ASSET, "USD");

        UUID clearingId = AccountService.cashClearingAccountId("USD");
        cashService.deposit(usdA.getId(), new DepositRequest(new BigDecimal("10.00"), null));
        cashService.deposit(usdB.getId(), new DepositRequest(new BigDecimal("15.00"), null));

        // Both deposits credited the same clearing account: 25.00 credited => signed -25.00.
        assertThat(ledgerService.getBalance(clearingId).signedBalance())
                .isEqualByComparingTo("-25.00");
    }

    @Test
    void depositIntoCreditNormalAccountCreditsIt() {
        Account liability = account(AccountType.LIABILITY, "USD");

        UUID txId = cashService.deposit(liability.getId(), new DepositRequest(new BigDecimal("75.00"), null));

        // Credit-normal account: a deposit should increase natural balance via a CREDIT.
        LedgerTransaction tx = ledgerService.getTransaction(txId);
        Posting leg = tx.getPostings().stream()
                .filter(p -> p.getAccountId().equals(liability.getId())).findFirst().orElseThrow();
        assertThat(leg.getDirection()).isEqualTo(PostingDirection.CREDIT);
        assertThat(ledgerService.getBalance(liability.getId()).naturalBalance())
                .isEqualByComparingTo("75.00");
    }
}
