package com.lukala.ledger.cash;

import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.cash.dto.DepositRequest;
import com.lukala.ledger.cash.dto.WithdrawRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.ledger.PostingDirection;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves cash into or out of an account while keeping the books balanced. Each
 * operation is recorded as a balanced double-entry transaction: the target
 * account is posted in the direction that increases (deposit) or decreases
 * (withdrawal) its natural balance, and the per-currency system "Cash Clearing"
 * account takes the opposite leg as the funding source.
 */
@Service
public class CashService {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    public CashService(AccountService accountService, LedgerService ledgerService) {
        this.accountService = accountService;
        this.ledgerService = ledgerService;
    }

    /**
     * Deposits cash into an account and returns the id of the posted ledger
     * transaction. Designed to run inside
     * {@link com.lukala.ledger.common.idempotency.IdempotencyService}.
     */
    @Transactional
    public UUID deposit(UUID accountId, DepositRequest request) {
        return move(accountId, request.amount(), request.description(), true, "Cash deposit to ");
    }

    /**
     * Withdraws cash from an account and returns the id of the posted ledger
     * transaction. Rejects the withdrawal if it would drive the account's natural
     * balance negative. Designed to run inside
     * {@link com.lukala.ledger.common.idempotency.IdempotencyService}.
     */
    @Transactional
    public UUID withdraw(UUID accountId, WithdrawRequest request) {
        return move(accountId, request.amount(), request.description(), false, "Cash withdrawal from ");
    }

    private UUID move(UUID accountId, BigDecimal amount, String description,
                      boolean increase, String defaultDescriptionPrefix) {
        Account account = accountService.get(accountId);
        String currency = account.getCurrency();

        // Provision (or reuse) the counter-leg account for this currency so the
        // movement has somewhere balanced to post against.
        Account cashClearing = accountService.getOrCreateCashClearingAccount(currency);

        if (!increase) {
            // Best-effort overdraft guard: don't let a withdrawal push the account
            // below zero. The definitive balance update is serialized under a lock
            // inside LedgerService.post; this pre-check gives a clean error.
            BigDecimal natural = ledgerService.getBalance(accountId).naturalBalance();
            if (natural.compareTo(amount) < 0) {
                throw new BusinessRuleException("INSUFFICIENT_FUNDS",
                        "Account " + accountId + " has " + natural + " " + currency
                                + " available; cannot withdraw " + amount + ".");
            }
        }

        // Post the target in the direction that moves its natural balance the way
        // we want; the cash clearing account takes the mirror leg so debits == credits.
        boolean debitTarget = account.getType().isDebitNormal() == increase;
        PostingDirection intoAccount = debitTarget ? PostingDirection.DEBIT : PostingDirection.CREDIT;
        PostingDirection fromCash =
                intoAccount == PostingDirection.DEBIT ? PostingDirection.CREDIT : PostingDirection.DEBIT;

        String desc = description != null && !description.isBlank()
                ? description
                : defaultDescriptionPrefix + accountId;

        return ledgerService.post(new CreateTransactionRequest(
                currency,
                null,
                desc,
                List.of(
                        new PostingRequest(accountId, intoAccount, amount),
                        new PostingRequest(cashClearing.getId(), fromCash, amount))));
    }
}
