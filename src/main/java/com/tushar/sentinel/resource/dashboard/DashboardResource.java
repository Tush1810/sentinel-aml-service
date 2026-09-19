package com.tushar.sentinel.resource.dashboard;

import com.tushar.sentinel.model.response.dashboard.DashboardSummary;
import com.tushar.sentinel.model.response.dashboard.TransactionView;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.alert.Alert;
import com.tushar.sentinel.repository.alert.AlertRepository;
import com.tushar.sentinel.repository.alert.AlertStatus;
import com.tushar.sentinel.repository.customer.CustomerRepository;
import com.tushar.sentinel.repository.txn.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregates the dashboard needs: headline counts and a customer's transaction timeline. */
@Transactional(readOnly = true)
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardResource {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    public DashboardResource(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AlertRepository alertRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * ponytail: groups alerts in memory rather than with aggregate queries, which is fine at
     * prototype volumes. Push the grouping into SQL if the alert table grows large.
     */
    @GetMapping("/summary")
    public DashboardSummary summary() {
        List<Alert> alerts = alertRepository.findAll();
        return new DashboardSummary(
                customerRepository.count(),
                accountRepository.count(),
                transactionRepository.count(),
                alerts.size(),
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.OPEN).count(),
                countBy(alerts, alert -> alert.getSeverity().name()),
                countBy(alerts, Alert::getRuleCode),
                countBy(alerts, alert -> alert.getStatus().name()));
    }

    @GetMapping("/customers/{customerRef}/transactions")
    public List<TransactionView> timeline(@PathVariable String customerRef) {
        return transactionRepository.findTimeline(customerRef).stream()
                .map(TransactionView::from)
                .toList();
    }

    private Map<String, Long> countBy(List<Alert> alerts, Function<Alert, String> key) {
        return alerts.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
    }
}
