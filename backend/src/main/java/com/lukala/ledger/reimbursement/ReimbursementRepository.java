package com.lukala.ledger.reimbursement;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {
}
