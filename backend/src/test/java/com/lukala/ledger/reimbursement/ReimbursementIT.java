package com.lukala.ledger.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lukala.ledger.AbstractPostgresIT;
import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountService;
import com.lukala.ledger.account.AccountType;
import com.lukala.ledger.account.dto.CreateAccountRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.reimbursement.dto.SubmitReimbursementRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReimbursementIT extends AbstractPostgresIT {

    @Autowired AccountService accountService;
    @Autowired ReimbursementService reimbursementService;

    private Account account(AccountType type) {
        return accountService.create(new CreateAccountRequest("acct-" + UUID.randomUUID(), type, "USD"));
    }

    private Reimbursement submit() {
        Account funding = account(AccountType.ASSET);
        Account payee = account(AccountType.EXPENSE);
        return reimbursementService.submit(new SubmitReimbursementRequest(
                "alex", new BigDecimal("25.00"), "USD", "taxi", funding.getId(), payee.getId()));
    }

    @Test
    void submitApprovePayIssuesPaymentAndLinksIt() {
        Reimbursement r = submit();
        assertThat(r.getStatus()).isEqualTo(ReimbursementStatus.SUBMITTED);

        reimbursementService.approve(r.getId(), "manager");
        Reimbursement paid = reimbursementService.pay(r.getId());

        assertThat(paid.getStatus()).isEqualTo(ReimbursementStatus.PAID);
        assertThat(paid.getPaymentId()).isNotNull();
    }

    @Test
    void payingWithoutApprovalIsRejected() {
        Reimbursement r = submit();
        assertThatThrownBy(() -> reimbursementService.pay(r.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot pay");
    }

    @Test
    void rejectedReimbursementCannotBePaid() {
        Reimbursement r = submit();
        reimbursementService.reject(r.getId(), "manager");
        assertThatThrownBy(() -> reimbursementService.pay(r.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
