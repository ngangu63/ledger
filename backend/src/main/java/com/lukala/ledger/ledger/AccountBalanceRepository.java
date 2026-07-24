package com.lukala.ledger.ledger;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {

    /**
     * Fetch a balance row for update, taking a pessimistic write lock
     * ({@code SELECT ... FOR UPDATE}). This serializes concurrent postings to the
     * same account so debits/credits cannot race and lose an update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AccountBalance b where b.accountId = :accountId")
    Optional<AccountBalance> findByIdForUpdate(@Param("accountId") UUID accountId);
}
