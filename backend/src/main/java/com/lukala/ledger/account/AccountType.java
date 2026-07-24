package com.lukala.ledger.account;

/**
 * The five classical account types. Each has a "normal balance" side that
 * determines whether a debit or a credit increases the account's natural balance.
 *
 * <ul>
 *   <li>ASSET, EXPENSE — debit-normal (debits increase)</li>
 *   <li>LIABILITY, EQUITY, REVENUE — credit-normal (credits increase)</li>
 * </ul>
 */
public enum AccountType {
    ASSET(true),
    EXPENSE(true),
    LIABILITY(false),
    EQUITY(false),
    REVENUE(false);

    private final boolean debitNormal;

    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    public boolean isDebitNormal() {
        return debitNormal;
    }
}
