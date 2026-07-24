package com.lukala.ledger.banking;

import static org.assertj.core.api.Assertions.assertThat;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.banking.dto.InitiateTransferRequest;
import com.lukala.ledger.ledger.LedgerService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BankingIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired BankingService bankingService;
    @Autowired LedgerService ledgerService;

    private Account account(AccountType type) {
        return accountService.create(new CreateAccountRequest("acct-" + UUID.randomUUID(), type, "USD"));
    }

    private UUID initiate(UUID source, UUID dest) {
        return bankingService.initiate(new InitiateTransferRequest(
                BankRail.ACH, new BigDecimal("75.00"), "USD", source, dest, null));
    }

    @Test
    void initiationRecordsLedgerEntryAndIsPending() {
        Account operating = account(AccountType.EQUITY);
        Account vendorCash = account(AccountType.ASSET);

        UUID id = initiate(operating.getId(), vendorCash.getId());

        BankTransfer transfer = bankingService.get(id);
        assertThat(transfer.getStatus()).isEqualTo(BankTransferStatus.PENDING);
        assertThat(transfer.getTransactionId()).isNotNull();
        assertThat(ledgerService.getBalance(vendorCash.getId()).signedBalance())
                .isEqualByComparingTo("75.00");
    }

    @Test
    void settlementMarksTransferSettled() {
        Account operating = account(AccountType.EQUITY);
        Account vendorCash = account(AccountType.ASSET);
        UUID id = initiate(operating.getId(), vendorCash.getId());
        String providerRef = bankingService.get(id).getProviderRef();

        bankingService.applySettlement(providerRef, BankTransferStatus.SETTLED);

        BankTransfer transfer = bankingService.get(id);
        assertThat(transfer.getStatus()).isEqualTo(BankTransferStatus.SETTLED);
        assertThat(transfer.getSettledAt()).isNotNull();
        // Money stays committed on settlement.
        assertThat(ledgerService.getBalance(vendorCash.getId()).signedBalance())
                .isEqualByComparingTo("75.00");
    }

    @Test
    void returnReversesTheLedgerEntry() {
        Account operating = account(AccountType.EQUITY);
        Account vendorCash = account(AccountType.ASSET);
        UUID id = initiate(operating.getId(), vendorCash.getId());
        String providerRef = bankingService.get(id).getProviderRef();

        bankingService.applySettlement(providerRef, BankTransferStatus.RETURNED);

        assertThat(bankingService.get(id).getStatus()).isEqualTo(BankTransferStatus.RETURNED);
        assertThat(ledgerService.getBalance(vendorCash.getId()).signedBalance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void duplicateSettlementCallbackIsIgnored() {
        Account operating = account(AccountType.EQUITY);
        Account vendorCash = account(AccountType.ASSET);
        UUID id = initiate(operating.getId(), vendorCash.getId());
        String providerRef = bankingService.get(id).getProviderRef();

        bankingService.applySettlement(providerRef, BankTransferStatus.SETTLED);
        // A repeated/late callback must not flip an already-terminal transfer.
        bankingService.applySettlement(providerRef, BankTransferStatus.RETURNED);

        assertThat(bankingService.get(id).getStatus()).isEqualTo(BankTransferStatus.SETTLED);
        assertThat(ledgerService.getBalance(vendorCash.getId()).signedBalance())
                .isEqualByComparingTo("75.00");
    }
}
