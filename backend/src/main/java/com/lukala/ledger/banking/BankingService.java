package com.lukala.ledger.banking;

import com.lukala.ledger.banking.connector.BankConnector;
import com.lukala.ledger.banking.connector.BankTransferCommand;
import com.lukala.ledger.banking.connector.BankTransferResult;
import com.lukala.ledger.banking.dto.InitiateTransferRequest;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.common.money.Money;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.ledger.PostingDirection;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates bank transfers: submit to the rail, record the money movement in the
 * ledger at initiation, then apply the terminal state from a settlement callback.
 * A RETURNED transfer is unwound by posting an offsetting ledger entry — the same
 * reversal path payments use, never bespoke posting logic.
 */
@Service
public class BankingService {

    private final BankTransferRepository transferRepository;
    private final BankConnector bankConnector;
    private final LedgerService ledgerService;

    public BankingService(BankTransferRepository transferRepository,
                          BankConnector bankConnector,
                          LedgerService ledgerService) {
        this.transferRepository = transferRepository;
        this.bankConnector = bankConnector;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public UUID initiate(InitiateTransferRequest request) {
        Money amount = Money.of(request.amount(), request.currency());
        BankTransfer transfer = new BankTransfer(UUID.randomUUID(), request.rail(), amount,
                request.sourceAccountId(), request.destinationAccountId(), request.externalRef());
        transferRepository.save(transfer);

        BankTransferResult result = bankConnector.initiateTransfer(new BankTransferCommand(
                transfer.getId(), request.rail(), amount,
                request.externalRef() != null ? request.externalRef() : "bank:" + transfer.getId()));

        // Record the money movement at initiation: debit destination, credit source.
        UUID txId = ledgerService.post(new CreateTransactionRequest(
                request.currency(),
                "bank:" + transfer.getId(),
                request.rail() + " transfer " + transfer.getId(),
                List.of(
                        new PostingRequest(request.destinationAccountId(), PostingDirection.DEBIT,
                                request.amount()),
                        new PostingRequest(request.sourceAccountId(), PostingDirection.CREDIT,
                                request.amount()))));

        transfer.markInitiated(bankConnector.name(), result.providerRef(), txId);
        transferRepository.save(transfer);
        return transfer.getId();
    }

    /**
     * Applies a settlement callback. SETTLED marks the transfer settled; RETURNED
     * posts an offsetting ledger entry and marks it returned. Terminal-state
     * transfers are left unchanged so repeated callbacks are safe (idempotent).
     */
    @Transactional
    public void applySettlement(String providerRef, BankTransferStatus status) {
        BankTransfer transfer = transferRepository.findByProviderRef(providerRef)
                .orElseThrow(() -> ResourceNotFoundException.of("BankTransfer(providerRef)", providerRef));

        if (!transfer.isPending()) {
            // Already terminal — ignore duplicate/late callbacks.
            return;
        }
        switch (status) {
            case SETTLED -> transfer.markSettled(Instant.now());
            case RETURNED -> {
                ledgerService.reverse(transfer.getTransactionId(), "bank-return:" + transfer.getId());
                transfer.markReturned();
            }
            case PENDING -> throw new BusinessRuleException("INVALID_SETTLEMENT_STATUS",
                    "A settlement callback must be SETTLED or RETURNED, not PENDING.");
        }
        transferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public BankTransfer get(UUID id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BankTransfer", id));
    }
}
