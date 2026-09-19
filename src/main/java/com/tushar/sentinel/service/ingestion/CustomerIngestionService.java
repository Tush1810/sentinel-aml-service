package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import com.tushar.sentinel.repository.customer.KycStatus;
import com.tushar.sentinel.repository.customer.RiskRating;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads customer KYC records. Best-effort: valid rows are kept, rejected rows are reported. */
@Service
public class CustomerIngestionService {

    private static final Logger log = LoggerFactory.getLogger(CustomerIngestionService.class);

    private final CustomerRepository customerRepository;

    public CustomerIngestionService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BatchResult ingestCsv(InputStream inputStream) {
        CsvFile csv = new CsvFile(inputStream);
        BatchResult result = csv.load("CUSTOMER", "customer_id", row -> {
            String customerRef = csv.get(row, "customer_id");
            if (customerRef == null) {
                throw new ValidationException("Missing customer reference");
            }
            if (customerRepository.existsByCustomerRef(customerRef)) {
                throw new ValidationException("Customer " + customerRef + " already ingested");
            }
            customerRepository.save(toCustomer(csv, row, customerRef));
        });

        log.info("Ingested customers batch {}; accepted={} rejected={}",
                result.batchId(), result.accepted(), result.rejected());
        return result;
    }

    private Customer toCustomer(CsvFile csv, String[] row, String customerRef) {
        Customer customer = new Customer();
        customer.setCustomerRef(customerRef);
        customer.setFirstName(csv.get(row, "first_name"));
        customer.setLastName(csv.get(row, "last_name"));
        customer.setGender(csv.get(row, "gender"));
        customer.setDateOfBirth(CsvValues.toDate(csv.get(row, "date_of_birth")));
        customer.setEmail(csv.get(row, "email"));
        customer.setPhoneNumber(csv.get(row, "phone_number"));
        customer.setCity(csv.get(row, "city"));
        customer.setState(csv.get(row, "state"));
        customer.setCountry(csv.get(row, "country"));
        customer.setPostalCode(csv.get(row, "postal_code"));
        customer.setOccupation(csv.get(row, "occupation"));
        customer.setAnnualIncome(CsvValues.toDecimal(csv.get(row, "annual_income")));
        customer.setMaritalStatus(csv.get(row, "marital_status"));
        customer.setEducationLevel(csv.get(row, "education_level"));
        customer.setEmploymentStatus(csv.get(row, "employment_status"));
        customer.setCustomerSince(CsvValues.toDate(csv.get(row, "customer_since")));
        customer.setCustomerSegment(csv.get(row, "customer_segment"));
        customer.setKycStatus(KycStatus.valueOf(csv.get(row, "kyc_status")));
        customer.setRiskRating(RiskRating.valueOf(csv.get(row, "risk_rating")));
        customer.setPoliticallyExposed(CsvValues.toBoolean(csv.get(row, "is_politically_exposed")));
        customer.setPreferredChannel(csv.get(row, "preferred_channel"));
        customer.setEmailVerified(CsvValues.toBoolean(csv.get(row, "email_verified")));
        customer.setPhoneVerified(CsvValues.toBoolean(csv.get(row, "phone_verified")));
        customer.setNumComplaintsLastYear(CsvValues.toIntOrZero(csv.get(row, "num_complaints_last_year")));
        return customer;
    }
}
