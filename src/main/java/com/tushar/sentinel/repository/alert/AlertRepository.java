package com.tushar.sentinel.repository.alert;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByDedupKey(String dedupKey);

    Optional<Alert> findByAlertRef(String alertRef);

    /** Analyst queue: highest risk first, per business rule 7. */
    @Query("select a from Alert a where (:status is null or a.status = :status) order by a.riskScore desc, a.detectedAt desc")
    List<Alert> findQueue(AlertStatus status, Pageable pageable);
}
