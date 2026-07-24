package com.lukala.ledger.account;

import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.ledger.AccountBalance;
import com.lukala.ledger.ledger.AccountBalanceRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountBalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Creates an account and its zero balance checkpoint atomically.
     */
    @Transactional
    public Account create(CreateAccountRequest request) {
        // Validate the currency is a real ISO-4217 code up-front.
        java.util.Currency.getInstance(request.currency());

        UUID id = UUID.randomUUID();
        Account account = new Account(id, request.name(), request.type(), request.currency());
        accountRepository.save(account);
        balanceRepository.save(new AccountBalance(id, request.currency()));
        return account;
    }

    /**
     * Returns the system "Cash Clearing" account for a currency, creating it (and
     * its zero balance) on first use. This account is the counter-leg for cash
     * deposits so the ledger stays balanced: a deposit debits the customer account
     * and credits this one. There is exactly one per currency, keyed by a
     * deterministic id derived from the currency so concurrent callers converge on
     * the same account.
     *
     * <p>Runs in its own transaction (REQUIRES_NEW) so provisioning commits
     * independently of the surrounding deposit/idempotency transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account getOrCreateCashClearingAccount(String currency) {
        java.util.Currency.getInstance(currency);
        UUID id = cashClearingAccountId(currency);
        return accountRepository.findById(id).orElseGet(() -> {
            Account account =
                    new Account(id, "Cash Clearing (" + currency + ")", AccountType.EQUITY, currency);
            accountRepository.save(account);
            balanceRepository.save(new AccountBalance(id, currency));
            return account;
        });
    }

    /** Deterministic id for the per-currency system cash clearing account. */
    public static UUID cashClearingAccountId(String currency) {
        return UUID.nameUUIDFromBytes(("system:cash-clearing:" + currency).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accountRepository.findAll();
    }
}
