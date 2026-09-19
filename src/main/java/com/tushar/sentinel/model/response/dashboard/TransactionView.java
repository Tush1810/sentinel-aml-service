package com.tushar.sentinel.model.response.dashboard;

import com.tushar.sentinel.repository.txn.Transaction;
import java.math.BigDecimal;
import java.time.Instant;

/** One row of a customer's transaction timeline. */
public record TransactionView(
        String txnRef,
        String accountRef,
        String direction,
        String txnType,
        BigDecimal amount,
        String currency,
        BigDecimal amountBase,
        String counterpartyName,
        String counterpartyCountry,
        String channel,
        Instant txnTimestamp) {

    public static TransactionView from(Transaction transaction) {
        return new TransactionView(
                transaction.getTxnRef(),
                transaction.getAccount().getAccountRef(),
                transaction.getDirection().name(),
                transaction.getTxnType().name(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getAmountBase(),
                transaction.getCounterpartyName(),
                transaction.getCounterpartyCountry(),
                transaction.getChannel(),
                transaction.getTxnTimestamp());
    }
}
