package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.operations.entity.Runbook;
import np.com.nepalupi.operations.enums.RunbookCategory;
import np.com.nepalupi.operations.repository.RunbookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Runbook Service — operational playbook management.
 * <p>
 * Pre-loaded runbooks for common UPI switch failure scenarios:
 * - Database connection pool exhaustion
 * - Kafka consumer lag spike
 * - Bank adapter timeout
 * - Settlement mismatch
 * - High fraud score transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunbookService {

    private final RunbookRepository runbookRepository;

    /**
     * Search runbooks by symptom keywords.
     */
    public List<Runbook> searchBySymptoms(String keyword) {
        return runbookRepository.searchByKeyword(keyword);
    }

    /**
     * Get runbooks by category.
     */
    public List<Runbook> getByCategory(RunbookCategory category) {
        return runbookRepository.findByCategory(category);
    }

    /**
     * Get all runbooks.
     */
    public List<Runbook> getAll() {
        return runbookRepository.findAll();
    }

    /**
     * Create or update a runbook.
     */
    @Transactional
    public Runbook save(Runbook runbook) {
        return runbookRepository.save(runbook);
    }

    /**
     * Mark a runbook as "used" for tracking purposes.
     */
    @Transactional
    public Runbook markUsed(UUID runbookId) {
        Runbook runbook = runbookRepository.findById(runbookId)
                .orElseThrow(() -> new IllegalArgumentException("Runbook not found"));
        runbook.setLastUsedAt(Instant.now());
        return runbookRepository.save(runbook);
    }
}
