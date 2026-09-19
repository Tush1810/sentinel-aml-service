package com.tushar.hackathon.model.response.casemanagement;

import com.tushar.hackathon.repository.auditlog.AuditLog;
import java.time.Instant;

/** One immutable state transition, as shown to an analyst or an auditor. */
public record AuditEntry(
        String entityType,
        String entityRef,
        String fromStatus,
        String toStatus,
        String actor,
        String reason,
        Instant occurredAt) {

    public static AuditEntry from(AuditLog entry) {
        return new AuditEntry(entry.getEntityType(), entry.getEntityRef(), entry.getFromStatus(),
                entry.getToStatus(), entry.getActor(), entry.getReason(), entry.getOccurredAt());
    }
}
