package com.lukala.ledger.account;

import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.ledger.AccountBalance;
import com.lukala.ledger.ledger.AccountBalanceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
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
