package com.tushar.hackathon.model.response.casemanagement;

import com.tushar.hackathon.repository.alert.Alert;
import com.tushar.hackathon.repository.amlcase.AmlCase;
import java.time.Instant;
import java.util.List;

/** An investigation and its decision, including the analyst who made it. */
public record CaseView(
        String caseRef,
        String customerRef,
        String status,
        String priority,
        String assignedTo,
        List<String> alertRefs,
        String disposition,
        String dispositionReason,
        String disposedBy,
        Instant disposedAt,
        Instant openedAt) {

    public static CaseView from(AmlCase amlCase) {
        return new CaseView(
                amlCase.getCaseRef(),
                amlCase.getCustomer().getCustomerRef(),
                amlCase.getStatus().name(),
                amlCase.getPriority().name(),
                amlCase.getAssignedTo(),
                amlCase.getAlerts().stream().map(Alert::getAlertRef).sorted().toList(),
                amlCase.getDisposition() == null ? null : amlCase.getDisposition().name(),
                amlCase.getDispositionReason(),
                amlCase.getDisposedBy(),
                amlCase.getDisposedAt(),
                amlCase.getOpenedAt());
    }
}
