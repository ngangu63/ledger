package com.lukala.ledger.reimbursement;

import com.lukala.ledger.reimbursement.dto.ReimbursementResponse;
import com.lukala.ledger.reimbursement.dto.SubmitReimbursementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reimbursement workflow endpoints: submit → approve/reject → pay. Paying an
 * approved reimbursement issues an OUTBOUND bank payment that is recorded in the
 * ledger. State transitions are enforced in {@link Reimbursement}.
 */
@RestController
@RequestMapping("/api/v1/reimbursements")
@Tag(name = "Reimbursements", description = "Employee/vendor reimbursement workflow")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @PostMapping
    @Operation(summary = "Submit a reimbursement request")
    public ResponseEntity<ReimbursementResponse> submit(
            @Valid @RequestBody SubmitReimbursementRequest request) {
        Reimbursement reimbursement = reimbursementService.submit(request);
        return ResponseEntity
                .created(URI.create("/api/v1/reimbursements/" + reimbursement.getId()))
                .body(ReimbursementResponse.from(reimbursement));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a submitted reimbursement")
    public ReimbursementResponse approve(@PathVariable UUID id, @RequestParam String approver) {
        return ReimbursementResponse.from(reimbursementService.approve(id, approver));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a submitted reimbursement")
    public ReimbursementResponse reject(@PathVariable UUID id, @RequestParam String approver) {
        return ReimbursementResponse.from(reimbursementService.reject(id, approver));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an approved reimbursement (issues a bank payment)")
    public ReimbursementResponse pay(@PathVariable UUID id) {
        return ReimbursementResponse.from(reimbursementService.pay(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reimbursement by id")
    public ReimbursementResponse get(@PathVariable UUID id) {
        return ReimbursementResponse.from(reimbursementService.get(id));
    }
}
