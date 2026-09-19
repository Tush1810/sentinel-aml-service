package com.tushar.hackathon.resource.alert;

import com.tushar.hackathon.exception.ResourceNotFoundException;
import com.tushar.hackathon.model.response.alert.AlertView;
import com.tushar.hackathon.repository.alert.AlertRepository;
import com.tushar.hackathon.repository.alert.AlertStatus;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analyst queue and alert detail. Reads run in a transaction because open-in-view is off,
 * so the lazy customer and evidence associations resolve before the view is built.
 */
@Transactional(readOnly = true)
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertResource {

    private static final int DEFAULT_SIZE = 50;

    private final AlertRepository alertRepository;

    public AlertResource(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Highest risk first, per business rule 7. Names are masked in this view. */
    @GetMapping
    public List<AlertView> queue(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return alertRepository.findQueue(status, PageRequest.of(0, size)).stream()
                .map(AlertView::masked)
                .toList();
    }

    /** Full customer detail is restricted to ADMIN; an analyst still sees the masked name. */
    @GetMapping("/{alertRef}")
    public AlertView detail(@PathVariable String alertRef, Authentication authentication) {
        boolean unmask = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return alertRepository.findByAlertRef(alertRef)
                .map(unmask ? AlertView::full : AlertView::masked)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertRef + " does not exist"));
    }
}
