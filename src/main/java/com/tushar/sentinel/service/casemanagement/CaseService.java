package com.tushar.sentinel.service.casemanagement;

import com.tushar.sentinel.exception.ResourceNotFoundException;
import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.casemanagement.AuditEntry;
import com.tushar.sentinel.model.response.casemanagement.CaseView;
import com.tushar.sentinel.repository.alert.Alert;
import com.tushar.sentinel.repository.alert.AlertRepository;
import com.tushar.sentinel.repository.alert.AlertStatus;
import com.tushar.sentinel.repository.amlcase.AmlCase;
import com.tushar.sentinel.repository.amlcase.CasePriority;
import com.tushar.sentinel.repository.amlcase.CaseRepository;
import com.tushar.sentinel.repository.amlcase.CaseStatus;
import com.tushar.sentinel.repository.amlcase.Disposition;
import com.tushar.sentinel.repository.auditlog.AuditLog;
import com.tushar.sentinel.repository.auditlog.AuditLogRepository;
import com.tushar.sentinel.repository.customer.Customer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bundles a customer's alerts into one investigation carrying one decision. Alerts and cases
 * are only ever moved, never removed, and every move is written to the audit trail.
 */
@Service
@Transactional(readOnly = true)
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private static final String ENTITY_CASE = "CASE";
    private static final String ENTITY_ALERT = "ALERT";
    private static final int REF_SUFFIX_LENGTH = 8;

    private final CaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final AuditLogRepository auditLogRepository;

    public CaseService(CaseRepository caseRepository, AlertRepository alertRepository,
                       AuditLogRepository auditLogRepository) {
        this.caseRepository = caseRepository;
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** Opens one investigation over the given alerts and takes them into review. */
    @Transactional
    public CaseView open(List<String> alertRefs, CasePriority priority, String actor) {
        List<Alert> alerts = alertRefs.stream().map(this::requireAlert).toList();
        Customer customer = alerts.getFirst().getCustomer();
        boolean spansCustomers = alerts.stream()
                .anyMatch(alert -> !alert.getCustomer().getId().equals(customer.getId()));
        if (spansCustomers) {
            throw new ValidationException("A case covers exactly one customer, but the alerts span several");
        }

        AmlCase amlCase = new AmlCase();
        amlCase.setCaseRef("CASE-" + UUID.randomUUID().toString().substring(0, REF_SUFFIX_LENGTH).toUpperCase());
        amlCase.setCustomer(customer);
        amlCase.setStatus(CaseStatus.OPEN);
        amlCase.setPriority(priority);
        amlCase.setOpenedAt(Instant.now());
        amlCase.getAlerts().addAll(alerts);
        AmlCase saved = caseRepository.save(amlCase);

        audit(ENTITY_CASE, saved.getCaseRef(), null, CaseStatus.OPEN.name(), actor,
                "Opened over " + alerts.size() + " alert(s)");
        alerts.forEach(alert -> moveAlert(alert, AlertStatus.IN_REVIEW, actor,
                "Bundled into case " + saved.getCaseRef()));

        log.info("Case {} opened by {} over alerts {}", saved.getCaseRef(), actor, alertRefs);
        return CaseView.from(saved);
    }

    @Transactional
    public CaseView assign(String caseRef, String assignee, String actor) {
        AmlCase amlCase = requireCase(caseRef);
        requireOpen(amlCase);
        CaseStatus from = amlCase.getStatus();
        amlCase.setAssignedTo(assignee);
        amlCase.setStatus(CaseStatus.IN_REVIEW);

        audit(ENTITY_CASE, caseRef, from.name(), CaseStatus.IN_REVIEW.name(), actor, "Assigned to " + assignee);
        log.info("Case {} assigned to {} by {}", caseRef, assignee, actor);
        return CaseView.from(amlCase);
    }

    /**
     * Records the single decision for the investigation and closes its alerts with the same
     * reason. The alerts stay in the system; only their status changes.
     */
    @Transactional
    public CaseView dispose(String caseRef, Disposition disposition, String reason, String actor) {
        AmlCase amlCase = requireCase(caseRef);
        requireOpen(amlCase);

        boolean escalated = disposition == Disposition.ESCALATED_TO_SAR;
        CaseStatus from = amlCase.getStatus();
        CaseStatus to = escalated ? CaseStatus.ESCALATED_TO_SAR : CaseStatus.CLOSED;
        amlCase.setStatus(to);
        amlCase.setDisposition(disposition);
        amlCase.setDispositionReason(reason);
        amlCase.setDisposedBy(actor);
        amlCase.setDisposedAt(Instant.now());

        audit(ENTITY_CASE, caseRef, from.name(), to.name(), actor, disposition + ": " + reason);
        AlertStatus alertOutcome = escalated ? AlertStatus.ESCALATED : AlertStatus.CLOSED;
        amlCase.getAlerts().forEach(alert -> moveAlert(alert, alertOutcome, actor, disposition + ": " + reason));

        log.info("Case {} disposed as {} by {}", caseRef, disposition, actor);
        return CaseView.from(amlCase);
    }

    public List<CaseView> queue(CaseStatus status, int size) {
        return caseRepository.findQueue(status, PageRequest.of(0, size)).stream()
                .map(CaseView::from)
                .toList();
    }

    public CaseView find(String caseRef) {
        return CaseView.from(requireCase(caseRef));
    }

    /** The trail for the case itself plus every alert it bundles, oldest first. */
    public List<AuditEntry> trail(String caseRef) {
        AmlCase amlCase = requireCase(caseRef);
        List<String> refs = Stream.concat(
                        Stream.of(amlCase.getCaseRef()),
                        amlCase.getAlerts().stream().map(Alert::getAlertRef))
                .toList();
        return auditLogRepository.findTrail(refs).stream()
                .map(AuditEntry::from)
                .toList();
    }

    private void moveAlert(Alert alert, AlertStatus to, String actor, String reason) {
        AlertStatus from = alert.getStatus();
        alert.setStatus(to);
        audit(ENTITY_ALERT, alert.getAlertRef(), from.name(), to.name(), actor, reason);
    }

    private void audit(String entityType, String entityRef, String from, String to, String actor, String reason) {
        auditLogRepository.save(new AuditLog(entityType, entityRef, from, to, actor, reason));
    }

    /** A decision is final: re-deciding a closed case would overwrite the analyst on record. */
    private void requireOpen(AmlCase amlCase) {
        if (amlCase.getDisposition() != null) {
            throw new ValidationException("Case " + amlCase.getCaseRef() + " is already disposed as "
                    + amlCase.getDisposition());
        }
    }

    private AmlCase requireCase(String caseRef) {
        return caseRepository.findByCaseRef(caseRef)
                .orElseThrow(() -> new ResourceNotFoundException("Case " + caseRef + " does not exist"));
    }

    private Alert requireAlert(String alertRef) {
        return alertRepository.findByAlertRef(alertRef)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertRef + " does not exist"));
    }
}
