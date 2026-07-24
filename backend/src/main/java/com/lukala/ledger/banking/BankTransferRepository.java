package com.lukala.ledger.banking;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransferRepository extends JpaRepository<BankTransfer, UUID> {

    Optional<BankTransfer> findByProviderRef(String providerRef);
}
