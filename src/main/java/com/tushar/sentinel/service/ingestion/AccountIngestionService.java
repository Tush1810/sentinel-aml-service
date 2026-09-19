package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.RowError;
import com.tushar.sentinel.repository.account.Account;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.account.AccountStatus;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads account metadata. Rows whose customer is unknown are rejected, not silently dropped. */
@Service
public class AccountIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AccountIngestionService.class);
    private static final int HEADER_OFFSET = 2;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountIngestionService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BatchResult ingestCsv(InputStream inputStream) {
        long startedAt = System.currentTimeMillis();
        String batchId = "ING-ACCT-" + UUID.randomUUID().toString().substring(0, 8);
        CsvFile csv = new CsvFile(inputStream);
        List<RowError> errors = new ArrayList<>();
        int accepted = 0;

        for (int i = 0; i < csv.rows().size(); i++) {
            String[] row = csv.rows().get(i);
            int rowNumber = i + HEADER_OFFSET;
            String accountRef = csv.get(row, "account_id");
            String customerRef = csv.get(row, "customer_id");
            try {
                if (accountRef == null || customerRef == null) {
                    errors.add(new RowError(rowNumber, accountRef, "account_id", "Missing account or customer reference"));
                    continue;
                }
                if (accountRepository.existsByAccountRef(accountRef)) {
                    errors.add(new RowError(rowNumber, accountRef, "account_id", "Already ingested"));
                    continue;
                }
                Optional<Customer> customer = customerRepository.findByCustomerRef(customerRef);
                if (customer.isEmpty()) {
                    errors.add(new RowError(rowNumber, accountRef, "customer_id",
                            "Customer " + customerRef + " does not exist"));
                    continue;
                }
                accountRepository.save(toAccount(csv, row, accountRef, customer.get()));
                accepted++;
            } catch (RuntimeException e) {
                errors.add(new RowError(rowNumber, accountRef, null, e.getMessage()));
            }
        }

        log.info("Ingested accounts batch {}; accepted={} rejected={}", batchId, accepted, errors.size());
        return new BatchResult(batchId, "ACCOUNT", csv.rows().size(), accepted, errors.size(),
                System.currentTimeMillis() - startedAt, errors);
    }

    private Account toAccount(CsvFile csv, String[] row, String accountRef, Customer customer) {
        Account account = new Account();
        account.setAccountRef(accountRef);
        account.setCustomer(customer);
        account.setAccountType(csv.get(row, "account_type"));
        account.setAccountStatus(AccountStatus.valueOf(csv.get(row, "account_status")));
        account.setCurrency(csv.get(row, "currency"));
        account.setOpenDate(CsvValues.toDate(csv.get(row, "open_date")));
        account.setCloseDate(CsvValues.toDate(csv.get(row, "close_date")));
        account.setBranchCode(csv.get(row, "branch_code"));
        account.setBranchCity(csv.get(row, "branch_city"));
        account.setCurrentBalance(CsvValues.toDecimal(csv.get(row, "current_balance")));
        account.setAvgMonthlyBalance6m(CsvValues.toDecimal(csv.get(row, "avg_monthly_balance_6m")));
        account.setCreditLimit(CsvValues.toDecimal(csv.get(row, "credit_limit")));
        account.setCreditUtilizationPct(CsvValues.toDecimal(csv.get(row, "credit_utilization_pct")));
        account.setOverdraftEnabled(CsvValues.toBoolean(csv.get(row, "overdraft_enabled")));
        account.setCardType(csv.get(row, "card_type"));
        account.setJointAccount(CsvValues.toBoolean(csv.get(row, "is_joint_account")));
        account.setNumLinkedDevices(CsvValues.toInteger(csv.get(row, "num_linked_devices")));
        account.setMobileBankingEnrolled(CsvValues.toBoolean(csv.get(row, "mobile_banking_enrolled")));
        account.setLastLoginDate(CsvValues.toDate(csv.get(row, "last_login_date")));
        account.setAvgMonthlyTxnCount(CsvValues.toInteger(csv.get(row, "avg_monthly_txn_count")));
        account.setAccountTier(csv.get(row, "account_tier"));
        return account;
    }
}
