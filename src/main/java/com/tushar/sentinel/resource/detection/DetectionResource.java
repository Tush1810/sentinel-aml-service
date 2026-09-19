package com.tushar.sentinel.resource.detection;

import com.tushar.sentinel.repository.txn.Transaction;
import com.tushar.sentinel.repository.txn.TransactionRepository;
import com.tushar.sentinel.service.detection.DetectionEngine;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Re-runs detection over every stored transaction. Needed after a rule threshold changes,
 * and after a bulk load that wrote rows straight to the database.
 */
@RestController
@RequestMapping("/api/v1/detection")
public class DetectionResource {

    private static final Logger log = LoggerFactory.getLogger(DetectionResource.class);

    private final TransactionRepository transactionRepository;
    private final DetectionEngine detectionEngine;

    public DetectionResource(TransactionRepository transactionRepository, DetectionEngine detectionEngine) {
        this.transactionRepository = transactionRepository;
        this.detectionEngine = detectionEngine;
    }

    /** Replays in business-time order, so windowed rules see the history they would have seen live. */
    @PostMapping("/run")
    @Transactional
    public Map<String, Object> run() {
        long startedAt = System.currentTimeMillis();
        List<Transaction> transactions = transactionRepository.findAllByOrderByTxnTimestampAsc();
        int alerts = 0;
        for (Transaction transaction : transactions) {
            alerts += detectionEngine.evaluate(transaction).size();
        }
        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("Detection re-run over {} transactions raised {} alerts in {} ms",
                transactions.size(), alerts, durationMs);
        return Map.of("transactionsEvaluated", transactions.size(),
                "ruleFirings", alerts, "durationMs", durationMs);
    }
}
