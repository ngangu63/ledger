package com.lukala.ledger.ledger;

import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountRepository;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.common.exception.UnbalancedTransactionException;
import com.lukala.ledger.common.money.Money;
import com.lukala.ledger.ledger.dto.BalanceResponse;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The posting engine — the one place money movement is recorded.
 *
 * <p>Every transaction is validated to be balanced (debits == credits) and
 * single-currency before any row is written. Postings are append-only; balance
 * checkpoints are updated in the same DB transaction under a pessimistic lock so
 * concurrent postings to an account cannot lose an update or double-spend.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final AccountBalanceRepository balanceRepository;

    public LedgerService(AccountRepository accountRepository,
                         LedgerTransactionRepository transactionRepository,
                         AccountBalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Posts a balanced, single-currency transaction and returns its id.
     * Designed to run inside {@link com.lukala.ledger.common.idempotency.IdempotencyService}.
     */
    @Transactional
    public UUID post(CreateTransactionRequest request) {
        String currency = requireCurrency(request.currency());

        if (request.externalRef() != null && !request.externalRef().isBlank()
                && transactionRepository.findByExternalRef(request.externalRef()).isPresent()) {
            throw new BusinessRuleException("EXTERNAL_REF_EXISTS",
                    "A transaction with externalRef '" + request.externalRef() + "' already exists.");
        }

        LedgerTransaction tx =
                new LedgerTransaction(UUID.randomUUID(), request.externalRef(), request.description());

        Money debits = Money.zero(currency);
        Money credits = Money.zero(currency);

        for (PostingRequest pr : request.postings()) {
            Account account = loadPostableAccount(pr.accountId(), currency);
            Money amount = Money.of(pr.amount(), currency);
            if (!amount.isPositive()) {
                throw new BusinessRuleException("Posting amount must be positive.");
            }
            tx.addPosting(new Posting(UUID.randomUUID(), tx, account.getId(), pr.direction(), amount));
            if (pr.direction() == PostingDirection.DEBIT) {
                debits = debits.add(amount);
            } else {
                credits = credits.add(amount);
            }
        }

        assertBalanced(debits, credits);

        transactionRepository.save(tx);
        applyToBalances(tx);
        return tx.getId();
    }

    /**
     * Reverses an existing posted transaction by writing an offsetting entry.
     * The original is never mutated beyond flipping its status to REVERSED.
     */
    @Transactional
    public UUID reverse(UUID transactionId, String description) {
        LedgerTransaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", transactionId));
        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw new BusinessRuleException("ALREADY_REVERSED",
                    "Transaction " + transactionId + " is already reversed.");
        }

        LedgerTransaction reversal = new LedgerTransaction(UUID.randomUUID(), null,
                description != null ? description : "Reversal of " + transactionId);
        reversal.setReversalOfId(original.getId());

        for (Posting p : original.getPostings()) {
            PostingDirection flipped = p.getDirection() == PostingDirection.DEBIT
                    ? PostingDirection.CREDIT : PostingDirection.DEBIT;
            reversal.addPosting(
                    new Posting(UUID.randomUUID(), reversal, p.getAccountId(), flipped, p.getMoney()));
        }

        original.markReversed();
        transactionRepository.save(reversal);
        applyToBalances(reversal);
        return reversal.getId();
    }

    @Transactional(readOnly = true)
    public LedgerTransaction getTransaction(UUID id) {
        LedgerTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
        // Initialize the lazy postings within this read transaction; open-in-view is
        // off, so the controller/DTO mapping runs after the session has closed.
        tx.getPostings().size();
        return tx;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", accountId));
        AccountBalance balance = balanceRepository.findById(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Balance", accountId));
        BigDecimal signed = balance.signedBalance().getAmount();
        BigDecimal natural = account.getType().isDebitNormal() ? signed : signed.negate();
        return new BalanceResponse(accountId, account.getCurrency(), signed, natural);
    }

    // --- internals ---

    private String requireCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new BusinessRuleException("CURRENCY_REQUIRED",
                    "A transaction currency is required.");
        }
        return currency;
    }

    private Account loadPostableAccount(UUID accountId, String currency) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", accountId));
        if (!account.isPostable()) {
            throw new BusinessRuleException("ACCOUNT_NOT_POSTABLE",
                    "Account " + accountId + " is " + account.getStatus() + " and cannot be posted to.");
        }
        if (!account.getCurrency().equals(currency)) {
            throw new BusinessRuleException("CURRENCY_MISMATCH",
                    "Account " + accountId + " is " + account.getCurrency()
                            + " but transaction is " + currency + ".");
        }
        return account;
    }

    private void assertBalanced(Money debits, Money credits) {
        if (debits.isZero() && credits.isZero()) {
            throw new UnbalancedTransactionException("Transaction has no monetary effect.");
        }
        if (!debits.equals(credits)) {
            throw new UnbalancedTransactionException(
                    "Debits (" + debits + ") do not equal credits (" + credits + ").");
        }
    }

    /**
     * Applies a transaction's net effect to each account's balance checkpoint.
     * Accounts are locked in a deterministic (id-sorted) order to avoid deadlocks
     * when two transactions touch the same pair of accounts.
     */
    private void applyToBalances(LedgerTransaction tx) {
        Map<UUID, Money> deltas = new LinkedHashMap<>();
        for (Posting p : tx.getPostings()) {
            deltas.merge(p.getAccountId(), p.signedAmount(), Money::add);
        }
        List<UUID> orderedAccountIds = new ArrayList<>(deltas.keySet());
        orderedAccountIds.sort(java.util.Comparator.comparing(UUID::toString));

        for (UUID accountId : orderedAccountIds) {
            AccountBalance balance = balanceRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Balance", accountId));
            balance.apply(deltas.get(accountId));
            balanceRepository.save(balance);
        }
    }
}
