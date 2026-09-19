package com.tushar.sentinel.repository.txn;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByTxnRef(String txnRef);

    @Query("select count(t) from Transaction t where t.account.customer.id = :customerId")
    long countByCustomerId(Long customerId);

    /** Customer transaction timeline for the dashboard, newest first. */
    @Query("select t from Transaction t where t.account.customer.customerRef = :customerRef"
            + " order by t.txnTimestamp desc")
    List<Transaction> findTimeline(String customerRef);
}
