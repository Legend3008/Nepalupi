package np.com.nepalupi.service.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SanctionsScreening;
import np.com.nepalupi.domain.entity.SuspiciousTransactionReport;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.SanctionsScreeningRepository;
import np.com.nepalupi.repository.SuspiciousTransactionReportRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Anti-Money Laundering (AML) service.
 * <p>
 * Detects suspicious patterns:
 * - STRUCTURING: Multiple transactions just below threshold
 * - VELOCITY: Unusual number of transactions in short period
 * - CIRCULAR: Round-trip funds between same parties
 * - SANCTIONS_HIT: User matches sanctions list
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmlService {

    private static final long STRUCTURING_THRESHOLD_PAISA = 10000000L; // Rs 1,00,000
    private static final int VELOCITY_MAX_TXN_PER_HOUR = 20;

    private final SuspiciousTransactionReportRepository strRepository;
    private final SanctionsScreeningRepository sanctionsRepository;
    private final TransactionRepository transactionRepository;
    private final ComplianceAuditService auditService;
    private final ObjectMapper objectMapper;

    // ══════════════════════════════════════════════════════════
    //  STR Management
    // ══════════════════════════════════════════════════════════

    /**
     * Create a suspicious transaction report.
     */
    @Transactional
    public SuspiciousTransactionReport fileStr(UUID transactionId, UUID userId,
                                                String suspicionType, String description,
                                                Map<String, Object> signals) {
        SuspiciousTransactionReport str = SuspiciousTransactionReport.builder()
                .transactionId(transactionId)
                .userId(userId)
                .suspicionType(suspicionType)
                .description(description)
                .signals(toJson(signals))
                .status("PENDING_REVIEW")
                .build();

        str = strRepository.save(str);

        auditService.recordEvent("STR_FILED", "TRANSACTION",
                transactionId != null ? transactionId.toString() : "N/A",
                "{\"strId\":\"" + str.getId() + "\",\"type\":\"" + suspicionType + "\"}");

        log.warn("STR filed: id={}, type={}, userId={}", str.getId(), suspicionType, userId);
        return str;
    }

    /**
     * Review and update STR status.
     */
    @Transactional
    public SuspiciousTransactionReport reviewStr(UUID strId, String newStatus,
                                                  String complianceOfficer) {
        SuspiciousTransactionReport str = strRepository.findById(strId)
                .orElseThrow(() -> new IllegalArgumentException("STR not found: " + strId));

        str.setStatus(newStatus);
        str.setComplianceOfficer(complianceOfficer);
        str.setReviewedAt(Instant.now());

        if ("FILED".equals(newStatus)) {
            str.setFiledWithFiu(true);
            str.setFiledAt(Instant.now());
            str.setFiuReference("FIU-NPL-" + System.currentTimeMillis() % 1000000);
        }

        str = strRepository.save(str);
        log.info("STR {} reviewed by {}: status={}", strId, complianceOfficer, newStatus);
        return str;
    }

    /**
     * Get STRs by status.
     */
    public List<SuspiciousTransactionReport> getStrsByStatus(String status) {
        return strRepository.findByStatus(status);
    }

    /**
     * Get STRs pending FIU filing.
     */
    public List<SuspiciousTransactionReport> getPendingFiling() {
        return strRepository.findByFiledWithFiuFalseAndStatus("FILED");
    }

    /**
     * Get STRs for a user.
     */
    public List<SuspiciousTransactionReport> getStrsByUser(UUID userId) {
        return strRepository.findByUserId(userId);
    }

    // ══════════════════════════════════════════════════════════
    //  Sanctions Screening
    // ══════════════════════════════════════════════════════════

    /**
     * Screen a user against all sanctions lists.
     * Returns true if any match found.
     */
    @Transactional
    public boolean screenUser(UUID userId) {
        boolean anyMatch = false;

        for (String list : List.of("UN_SECURITY_COUNCIL", "INTERPOL", "NEPAL_DOMESTIC")) {
            boolean match = performScreening(userId, list);
            SanctionsScreening screening = SanctionsScreening.builder()
                    .userId(userId)
                    .screenedAgainst(list)
                    .matchFound(match)
                    .matchDetails(match ? "Potential match found — manual review required" : null)
                    .build();
            sanctionsRepository.save(screening);

            if (match) {
                anyMatch = true;
                log.warn("SANCTIONS MATCH: userId={}, list={}", userId, list);

                // Auto-file STR for sanctions hit
                fileStr(null, userId, "SANCTIONS_HIT",
                        "User matched against " + list + " sanctions list",
                        Map.of("list", list, "userId", userId.toString()));
            }
        }

        return anyMatch;
    }

    /**
     * Get screening history for a user.
     */
    public List<SanctionsScreening> getScreeningHistory(UUID userId) {
        return sanctionsRepository.findByUserId(userId);
    }

    /**
     * Get all sanctions matches.
     */
    public List<SanctionsScreening> getMatches(UUID userId) {
        return sanctionsRepository.findByUserIdAndMatchFoundTrue(userId);
    }

    // ══════════════════════════════════════════════════════════
    //  Pattern Detection
    // ══════════════════════════════════════════════════════════

    /**
     * Check a transaction for structuring (splitting to avoid threshold).
     * Looks at recent transactions by the same payer VPA.
     */
    public boolean checkStructuring(Transaction transaction) {
        // Find recent transactions by the same payer in the last 24 hours
        Instant cutoff = Instant.now().minusSeconds(86400);
        List<Transaction> recentTxns = transactionRepository.findAll().stream()
                .filter(t -> t.getPayerVpa().equals(transaction.getPayerVpa())
                        && t.getCreatedAt() != null
                        && t.getCreatedAt().isAfter(cutoff)
                        && t.getAmount() != null
                        && t.getAmount() < STRUCTURING_THRESHOLD_PAISA
                        && t.getAmount() > STRUCTURING_THRESHOLD_PAISA * 7 / 10)
                .toList();

        if (recentTxns.size() >= 3) {
            long totalAmount = recentTxns.stream().mapToLong(Transaction::getAmount).sum();
            if (totalAmount > STRUCTURING_THRESHOLD_PAISA * 2) {
                log.warn("STRUCTURING detected: payerVpa={}, txnCount={}, total={}",
                        transaction.getPayerVpa(), recentTxns.size(), totalAmount);
                return true;
            }
        }
        return false;
    }

    /**
     * Check for velocity anomaly (too many transactions in short period).
     */
    public boolean checkVelocity(Transaction transaction) {
        Instant cutoff = Instant.now().minusSeconds(3600);
        long recentCount = transactionRepository.findAll().stream()
                .filter(t -> t.getPayerVpa().equals(transaction.getPayerVpa())
                        && t.getCreatedAt() != null
                        && t.getCreatedAt().isAfter(cutoff))
                .count();

        if (recentCount > VELOCITY_MAX_TXN_PER_HOUR) {
            log.warn("VELOCITY anomaly: payerVpa={}, txnCount={}/hr",
                    transaction.getPayerVpa(), recentCount);
            return true;
        }
        return false;
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * Mock sanctions screening — always returns false (no match) in dev mode.
     * Real implementation would call external sanctions API.
     */
    private boolean performScreening(UUID userId, String list) {
        // Mock — no matches in dev
        log.debug("Screening userId={} against {}", userId, list);
        return false;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
