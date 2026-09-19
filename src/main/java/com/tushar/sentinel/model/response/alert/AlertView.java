package com.tushar.sentinel.model.response.alert;

import com.tushar.sentinel.repository.alert.Alert;
import com.tushar.sentinel.repository.customer.Customer;
import com.tushar.sentinel.repository.txn.Transaction;
import java.time.Instant;
import java.util.List;

/**
 * Analyst queue row. The customer name is masked here and revealed only in the detail
 * view, per business rule 8 — masking server side, because hiding it in the UI alone
 * would still put the real value on the wire.
 */
public record AlertView(
        String alertRef,
        String ruleCode,
        String typology,
        int riskScore,
        String severity,
        String status,
        String customerRef,
        String customerName,
        String accountRef,
        String explanation,
        List<String> evidence,
        Instant detectedAt) {

    public static AlertView masked(Alert alert) {
        return of(alert, maskName(alert.getCustomer()));
    }

    public static AlertView full(Alert alert) {
        Customer customer = alert.getCustomer();
        return of(alert, customer.getFirstName() + " " + customer.getLastName());
    }

    private static AlertView of(Alert alert, String customerName) {
        return new AlertView(
                alert.getAlertRef(), alert.getRuleCode(), alert.getTypology(), alert.getRiskScore(),
                alert.getSeverity().name(), alert.getStatus().name(),
                alert.getCustomer().getCustomerRef(), customerName,
                alert.getAccount() == null ? null : alert.getAccount().getAccountRef(),
                alert.getExplanation(),
                alert.getEvidence().stream().map(Transaction::getTxnRef).sorted().toList(),
                alert.getDetectedAt());
    }

    private static String maskName(Customer customer) {
        String lastName = customer.getLastName();
        if (lastName == null || lastName.isBlank()) {
            return customer.getFirstName();
        }
        return customer.getFirstName() + " " + lastName.charAt(0) + ".";
    }
}
