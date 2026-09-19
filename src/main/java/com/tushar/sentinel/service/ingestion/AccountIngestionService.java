package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.exception.ResourceNotFoundException;
import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.repository.account.Account;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.account.AccountStatus;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads account metadata. Rows whose customer is unknown are rejected, not silently dropped. */
@Service
public class AccountIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AccountIngestionService.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountIngestionService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BatchResult ingestCsv(InputStream inputStream) {
        CsvFile csv = new CsvFile(inputStream);
        BatchResult result = csv.load("ACCOUNT", "account_id", row -> {
            String accountRef = csv.get(row, "account_id");
            String customerRef = csv.get(row, "customer_id");
            if (accountRef == null || customerRef == null) {
                throw new ValidationException("Missing account or customer reference");
            }
            if (accountRepository.existsByAccountRef(accountRef)) {
                throw new ValidationException("Account " + accountRef + " already ingested");
            }
            Customer customer = customerRepository.findByCustomerRef(customerRef)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer " + customerRef + " does not exist"));
            accountRepository.save(toAccount(csv, row, accountRef, customer));
        });

        log.info("Ingested accounts batch {}; accepted={} rejected={}",
                result.batchId(), result.accepted(), result.rejected());
        return result;
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
