package com.tushar.hackathon.repository.amlcase;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CaseRepository extends JpaRepository<AmlCase, Long> {

    Optional<AmlCase> findByCaseRef(String caseRef);

    /** Investigation queue: newest first, optionally narrowed to one status. */
    @Query("select c from AmlCase c where (:status is null or c.status = :status) order by c.openedAt desc")
    List<AmlCase> findQueue(CaseStatus status, Pageable pageable);
}
