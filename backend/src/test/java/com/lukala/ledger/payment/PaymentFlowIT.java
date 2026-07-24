package com.lukala.ledger.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.payment.dto.CreatePaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentFlowIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired PaymentService paymentService;
    @Autowired LedgerService ledgerService;

    private Account account(AccountType type) {
        return accountService.create(new CreateAccountRequest("acct-" + UUID.randomUUID(), type, "USD"));
    }

    private CreatePaymentRequest charge(UUID source, UUID dest, String token) {
        return new CreatePaymentRequest(PaymentDirection.INBOUND, PaymentMethod.CARD,
                new BigDecimal("50.00"), "USD", source, dest, token, null);
    }

    @Test
    void capturedPaymentRecordsLedgerEntry() {
        Account customer = account(AccountType.REVENUE);
        Account cash = account(AccountType.ASSET);

        UUID paymentId = paymentService.charge(charge(customer.getId(), cash.getId(), null));

        Payment payment = paymentService.get(paymentId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getTransactionId()).isNotNull();
        assertThat(ledgerService.getBalance(cash.getId()).signedBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void declinedPaymentWritesNoLedgerEntry() {
        Account customer = account(AccountType.REVENUE);
        Account cash = account(AccountType.ASSET);

        UUID paymentId = paymentService.charge(charge(customer.getId(), cash.getId(), "decline"));

        Payment payment = paymentService.get(paymentId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getTransactionId()).isNull();
        assertThat(ledgerService.getBalance(cash.getId()).signedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void refundReversesTheLedgerEntry() {
        Account customer = account(AccountType.REVENUE);
        Account cash = account(AccountType.ASSET);
        UUID paymentId = paymentService.charge(charge(customer.getId(), cash.getId(), null));

        paymentService.refund(paymentId);

        assertThat(paymentService.get(paymentId).getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(ledgerService.getBalance(cash.getId()).signedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void refundingAFailedPaymentIsRejected() {
        Account customer = account(AccountType.REVENUE);
        Account cash = account(AccountType.ASSET);
        UUID paymentId = paymentService.charge(charge(customer.getId(), cash.getId(), "decline"));

        assertThatThrownBy(() -> paymentService.refund(paymentId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be refunded");
    }
}
