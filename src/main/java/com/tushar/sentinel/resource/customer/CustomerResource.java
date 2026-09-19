package com.tushar.sentinel.resource.customer;

import com.tushar.sentinel.exception.ResourceNotFoundException;
import com.tushar.sentinel.repository.account.Account;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.account.AccountStatus;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import com.tushar.sentinel.repository.customer.KycStatus;
import com.tushar.sentinel.repository.customer.RiskRating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the customers and accounts a transaction can be posted against, for the cases where
 * a CSV load is too heavy. Bulk loading stays the real path: see {@code /api/v1/ingestion}.
 *
 * <p>Only the fields that change detection are taken from the caller. Everything else is
 * filled with a sensible default, because a form asking for twenty-eight KYC fields would
 * not get used.
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerResource {

    private static final BigDecimal OPENING_BALANCE = new BigDecimal("500000");
    private static final String DEFAULT_ACCOUNT_TYPE = "SAVINGS";

    /**
     * ponytail: refs are seeded from the clock rather than a database sequence, so two
     * instances started in the same millisecond could collide. Use a sequence if this ever
     * runs as more than one process.
     */
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 100000);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerResource(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    /** Creates a customer together with one account, so they can hold transactions immediately. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CreatedCustomer create(@Valid @RequestBody NewCustomerRequest request) {
        Customer customer = new Customer();
        customer.setCustomerRef("CUST_S" + sequence.incrementAndGet());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setCity("Bengaluru");
        customer.setCountry("IN");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setCustomerSince(LocalDate.now().minusYears(2));
        customer.setCustomerSegment("RETAIL");
        customer.setKycStatus(request.kycVerified() ? KycStatus.VERIFIED : KycStatus.PENDING);
        customer.setRiskRating(request.politicallyExposed() ? RiskRating.HIGH : RiskRating.LOW);
        customer.setPoliticallyExposed(request.politicallyExposed());
        customer.setEmailVerified(true);
        customer.setPhoneVerified(true);
        customerRepository.save(customer);

        Account account = newAccount(customer, DEFAULT_ACCOUNT_TYPE);
        accountRepository.save(account);
        return CreatedCustomer.of(customer, account);
    }

    /** Adds another account to an existing customer. */
    @PostMapping("/{customerRef}/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CreatedCustomer addAccount(@PathVariable String customerRef,
                                      @Valid @RequestBody NewAccountRequest request) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer " + customerRef + " does not exist"));
        Account account = newAccount(customer, request.accountType());
        accountRepository.save(account);
        return CreatedCustomer.of(customer, account);
    }

    private Account newAccount(Customer customer, String accountType) {
        Account account = new Account();
        account.setAccountRef("ACC_S" + sequence.incrementAndGet());
        account.setCustomer(customer);
        account.setAccountType(accountType == null || accountType.isBlank()
                ? DEFAULT_ACCOUNT_TYPE : accountType);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setCurrency("INR");
        account.setOpenDate(LocalDate.now().minusYears(2));
        account.setCurrentBalance(OPENING_BALANCE);
        return account;
    }

    /**
     * Politically exposed customers are created as HIGH risk, and unverified KYC adds its own
     * uplift, so both flags change the score of every alert this customer later raises.
     */
    public record NewCustomerRequest(@NotBlank String firstName, @NotBlank String lastName,
                                     boolean politicallyExposed, boolean kycVerified) {
    }

    public record NewAccountRequest(String accountType) {
    }

    public record CreatedCustomer(String customerRef, String accountRef, String name,
                                  boolean politicallyExposed, String kycStatus, String riskRating) {

        static CreatedCustomer of(Customer customer, Account account) {
            return new CreatedCustomer(
                    customer.getCustomerRef(), account.getAccountRef(),
                    customer.getFirstName() + " " + customer.getLastName(),
                    customer.isPoliticallyExposed(),
                    customer.getKycStatus().name(), customer.getRiskRating().name());
        }
    }
}
