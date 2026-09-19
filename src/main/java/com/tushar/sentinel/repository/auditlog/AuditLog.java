package com.tushar.sentinel.repository.auditlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** One recorded state transition of an alert or a case. Written once, never changed. */
@Entity
@Table(name = "audit_log")
@Immutable
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 16)
    private String entityType;

    @Column(name = "entity_ref", nullable = false, updatable = false, length = 48)
    private String entityRef;

    /** Null when the entity was created rather than moved. */
    @Column(name = "from_status", updatable = false, length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, updatable = false, length = 24)
    private String toStatus;

    /** Authenticated principal that made the change; never taken from a request body. */
    @Column(nullable = false, updatable = false, length = 64)
    private String actor;

    @Column(updatable = false, length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public AuditLog(String entityType, String entityRef, String fromStatus, String toStatus,
                    String actor, String reason) {
        this.entityType = entityType;
        this.entityRef = entityRef;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }
}
