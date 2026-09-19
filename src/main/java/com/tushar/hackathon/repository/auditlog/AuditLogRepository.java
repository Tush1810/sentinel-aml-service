package com.tushar.hackathon.repository.auditlog;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Chronological trail for a case and the alerts it bundles. */
    @Query("select e from AuditLog e where e.entityRef in :entityRefs order by e.occurredAt asc, e.id asc")
    List<AuditLog> findTrail(Collection<String> entityRefs);
}
