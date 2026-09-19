package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.RowError;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import com.tushar.sentinel.repository.customer.KycStatus;
import com.tushar.sentinel.repository.customer.RiskRating;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads customer KYC records. Best-effort: valid rows are kept, rejected rows are reported. */
@Service
public class CustomerIngestionService {

    private static final Logger log = LoggerFactory.getLogger(CustomerIngestionService.class);
    private static final int HEADER_OFFSET = 2;

    private final CustomerRepository customerRepository;

    public CustomerIngestionService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BatchResult ingestCsv(InputStream inputStream) {
        long startedAt = System.currentTimeMillis();
        String batchId = "ING-CUST-" + UUID.randomUUID().toString().substring(0, 8);
        CsvFile csv = new CsvFile(inputStream);
        List<RowError> errors = new ArrayList<>();
        int accepted = 0;

        for (int i = 0; i < csv.rows().size(); i++) {
            String[] row = csv.rows().get(i);
            int rowNumber = i + HEADER_OFFSET;
            String customerRef = csv.get(row, "customer_id");
            try {
                if (customerRef == null) {
                    errors.add(new RowError(rowNumber, null, "customer_id", "Missing customer reference"));
                    continue;
                }
                if (customerRepository.existsByCustomerRef(customerRef)) {
                    errors.add(new RowError(rowNumber, customerRef, "customer_id", "Already ingested"));
                    continue;
                }
                customerRepository.save(toCustomer(csv, row, customerRef));
                accepted++;
            } catch (RuntimeException e) {
                errors.add(new RowError(rowNumber, customerRef, null, e.getMessage()));
            }
        }

        log.info("Ingested customers batch {}; accepted={} rejected={}", batchId, accepted, errors.size());
        return new BatchResult(batchId, "CUSTOMER", csv.rows().size(), accepted, errors.size(),
                System.currentTimeMillis() - startedAt, errors);
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
