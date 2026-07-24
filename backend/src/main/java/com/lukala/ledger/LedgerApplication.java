package com.lukala.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ledger — financial ledger and payment infrastructure.
 *
 * <p>Records money movement using immutable, double-entry accounting. Every
 * transaction is a balanced set of debit/credit postings; balances are derived
 * from the append-only posting log, never mutated in place.
 */
@SpringBootApplication
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
