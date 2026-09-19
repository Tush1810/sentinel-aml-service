package com.tushar.sentinel.resource.simulation;

import com.tushar.sentinel.exception.ResourceNotFoundException;
import com.tushar.sentinel.repository.account.Account;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.account.AccountStatus;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import com.tushar.sentinel.repository.customer.KycStatus;
import com.tushar.sentinel.repository.customer.RiskRating;
import com.tushar.sentinel.repository.txn.TxnDirection;
import com.tushar.sentinel.repository.txn.TxnType;
import com.tushar.sentinel.service.ingestion.IngestTransactionCommand;
import com.tushar.sentinel.service.ingestion.TransactionIngestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo aid. Creates a customer with a single account and replays a named laundering
 * pattern against it, so the detection engine can be shown from the dashboard rather
 * than from a REST client. Not part of the production surface.
 */
@RestController
@RequestMapping("/api/v1/simulation")
public class SimulationResource {

    private static final BigDecimal[] STRUCTURING_AMOUNTS = {
            new BigDecimal("9500"), new BigDecimal("9700"), new BigDecimal("9200")};
    private static final BigDecimal[] ROUND_AMOUNTS = {
            new BigDecimal("20000"), new BigDecimal("30000"), new BigDecimal("50000")};
    private static final BigDecimal HIGH_RISK_AMOUNT = new BigDecimal("25000");
    private static final BigDecimal LARGE_AMOUNT = new BigDecimal("75000");
    private static final BigDecimal RAPID_IN = new BigDecimal("200000");
    private static final BigDecimal RAPID_OUT = new BigDecimal("180000");
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("500000");
    private static final int MIN_DAY_OFFSET = 30;
    private static final int MAX_DAY_OFFSET = 900;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionIngestionService ingestionService;
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 100000);

    public SimulationResource(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionIngestionService ingestionService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.ingestionService = ingestionService;
    }

    /** Creates a customer and one account, filling everything the demo does not care about. */
    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SimulatedCustomer createCustomer(@Valid @RequestBody NewCustomerRequest request) {
        long id = sequence.incrementAndGet();
        Customer customer = new Customer();
        customer.setCustomerRef("CUST_S" + id);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setCity("Bengaluru");
        customer.setCountry("IN");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setCustomerSince(LocalDate.now().minusYears(2));
        customer.setCustomerSegment("RETAIL");
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setRiskRating(request.politicallyExposed() ? RiskRating.HIGH : RiskRating.LOW);
        customer.setPoliticallyExposed(request.politicallyExposed());
        customer.setEmailVerified(true);
        customer.setPhoneVerified(true);
        customerRepository.save(customer);

        Account account = newAccount(customer, "SAVINGS");
        accountRepository.save(account);

        return new SimulatedCustomer(customer.getCustomerRef(), account.getAccountRef(),
                customer.getFirstName() + " " + customer.getLastName(), customer.isPoliticallyExposed());
    }

    /** Adds another account to an existing customer, so it can hold transactions. */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SimulatedCustomer createAccount(@Valid @RequestBody NewAccountRequest request) {
        Customer customer = customerRepository.findByCustomerRef(request.customerRef())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer " + request.customerRef() + " does not exist"));
        Account account = newAccount(customer, request.accountType());
        accountRepository.save(account);
        return new SimulatedCustomer(customer.getCustomerRef(), account.getAccountRef(),
                customer.getFirstName() + " " + customer.getLastName(), customer.isPoliticallyExposed());
    }

    /** Accounts available to run a scenario against. */
    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public List<SimulatedCustomer> accounts() {
        return accountRepository.findAll().stream()
                .map(account -> new SimulatedCustomer(
                        account.getCustomer().getCustomerRef(), account.getAccountRef(),
                        account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName(),
                        account.getCustomer().isPoliticallyExposed()))
                .toList();
    }

    /**
     * Replays a pattern. Each run picks its own day, so transactions from an earlier run
     * cannot sit in the same detection window and complete the pattern early.
     */
    @PostMapping("/scenario")
    public ScenarioResult runScenario(@Valid @RequestBody ScenarioRequest request) {
        Instant day = Instant.now().plus(Duration.ofDays(
                ThreadLocalRandom.current().nextInt(MIN_DAY_OFFSET, MAX_DAY_OFFSET)));
        List<StepResult> steps = new ArrayList<>();

        switch (request.scenario()) {
            case STRUCTURING -> {
                for (int i = 0; i < STRUCTURING_AMOUNTS.length; i++) {
                    steps.add(fire(request.accountRef(), TxnDirection.CREDIT, TxnType.CASH_DEPOSIT,
                            STRUCTURING_AMOUNTS[i], "IN", day.plus(Duration.ofHours(i * 3L))));
                }
            }
            case ROUND_NUMBER -> {
                for (int i = 0; i < ROUND_AMOUNTS.length; i++) {
                    steps.add(fire(request.accountRef(), TxnDirection.DEBIT, TxnType.PAYMENT,
                            ROUND_AMOUNTS[i], "IN", day.plus(Duration.ofDays(i))));
                }
            }
            case HIGH_RISK_JURISDICTION -> steps.add(fire(request.accountRef(), TxnDirection.DEBIT,
                    TxnType.WIRE, HIGH_RISK_AMOUNT, "AE", day));
            case LARGE_TRANSACTION -> steps.add(fire(request.accountRef(), TxnDirection.DEBIT,
                    TxnType.TRANSFER, LARGE_AMOUNT, "IN", day));
            case RAPID_MOVEMENT -> {
                steps.add(fire(request.accountRef(), TxnDirection.CREDIT, TxnType.TRANSFER,
                        RAPID_IN, "IN", day));
                steps.add(fire(request.accountRef(), TxnDirection.DEBIT, TxnType.TRANSFER,
                        RAPID_OUT, "IN", day.plus(Duration.ofHours(8))));
            }
        }
        return new ScenarioResult(request.scenario().name(), steps);
    }

    private Account newAccount(Customer customer, String accountType) {
        Account account = new Account();
        account.setAccountRef("ACC_S" + sequence.incrementAndGet());
        account.setCustomer(customer);
        account.setAccountType(accountType == null || accountType.isBlank() ? "SAVINGS" : accountType);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setCurrency("INR");
        account.setOpenDate(LocalDate.now().minusYears(2));
        account.setCurrentBalance(OPENING_BALANCE);
        return account;
    }

    private StepResult fire(String accountRef, TxnDirection direction, TxnType type,
                            BigDecimal amount, String country, Instant at) {
        String ref = "SIM_" + sequence.incrementAndGet();
        ingestionService.ingestOne(new IngestTransactionCommand(
                ref, accountRef, direction, type, amount, "INR",
                country.equals("IN") ? "Domestic counterparty" : "Offshore counterparty",
                null, null, country, "ONLINE", "Simulated " + type.name(), at));
        return new StepResult(ref, direction.name(), amount, country, at);
    }

    public record NewAccountRequest(@NotBlank String customerRef, String accountType) {
    }

    public record NewCustomerRequest(@NotBlank String firstName, @NotBlank String lastName,
                                     boolean politicallyExposed) {
    }

    public record SimulatedCustomer(String customerRef, String accountRef, String name,
                                    boolean politicallyExposed) {
    }

    public record ScenarioRequest(@NotBlank String accountRef, Scenario scenario) {
    }

    public record ScenarioResult(String scenario, List<StepResult> steps) {
    }

    public record StepResult(String txnRef, String direction, BigDecimal amount, String country,
                             Instant txnTimestamp) {
    }

    /** Laundering patterns the dashboard can replay. */
    public enum Scenario {
        STRUCTURING,
        RAPID_MOVEMENT,
        HIGH_RISK_JURISDICTION,
        LARGE_TRANSACTION,
        ROUND_NUMBER
    }
}
