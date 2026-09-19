package com.tushar.hackathon.repository.amlcase;

import com.tushar.hackathon.repository.alert.Alert;
import com.tushar.hackathon.repository.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An analyst investigation bundling one or more {@link Alert}s about a customer.
 * Table is named {@code aml_case} because {@code case} is a SQL reserved word.
 */
@Entity
@Table(name = "aml_case")
@Getter
@Setter
@NoArgsConstructor
public class AmlCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_ref", nullable = false, unique = true, length = 48)
    private String caseRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CasePriority priority;

    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    /** Set once the investigation closes; the alert itself is never deleted. */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Disposition disposition;

    @Column(name = "disposition_reason", length = 1000)
    private String dispositionReason;

    /** Analyst identity retained for audit, per the never-silently-delete rule. */
    @Column(name = "disposed_by", length = 64)
    private String disposedBy;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "case_alert",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "alert_id"))
    private Set<Alert> alerts = new LinkedHashSet<>();

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
