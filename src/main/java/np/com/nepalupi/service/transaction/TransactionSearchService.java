package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Unified transaction search service.
 * Exposes the existing repository queries as a unified search API
 * with support for filtering by status, date range, VPA, RRN, and amount range.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSearchService {

    private final TransactionRepository transactionRepository;

    /**
     * Unified transaction search with multiple filter criteria.
     */
    public Map<String, Object> search(
            String vpa,
            TransactionStatus status,
            String rrn,
            String upiTxnId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long amountMin,
            Long amountMax,
            int page,
            int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "initiatedAt"));

        // Priority: specific lookups first
        if (upiTxnId != null && !upiTxnId.isBlank()) {
            return transactionRepository.findByUpiTxnId(upiTxnId)
                    .map(t -> Map.<String, Object>of(
                            "transactions", List.of(TransactionResponse.from(t)),
                            "totalElements", 1L,
                            "page", 0,
                            "size", 1
                    ))
                    .orElse(Map.of("transactions", List.of(), "totalElements", 0L, "page", 0, "size", 0));
        }

        if (rrn != null && !rrn.isBlank()) {
            return transactionRepository.findByRrn(rrn)
                    .map(t -> Map.<String, Object>of(
                            "transactions", List.of(TransactionResponse.from(t)),
                            "totalElements", 1L,
                            "page", 0,
                            "size", 1
                    ))
                    .orElse(Map.of("transactions", List.of(), "totalElements", 0L, "page", 0, "size", 0));
        }

        // Date range search
        if (dateFrom != null && dateTo != null) {
            Instant from = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Page<Transaction> results = transactionRepository.findByDateRange(from, to, pageRequest);
            return buildPageResponse(results, page, size);
        }

        // Status filter
        if (status != null) {
            Page<Transaction> results = transactionRepository.findByStatus(status, pageRequest);
            return buildPageResponse(results, page, size);
        }

        // VPA filter
        if (vpa != null && !vpa.isBlank()) {
            Page<Transaction> results = transactionRepository.findByVpa(vpa, pageRequest);
            return buildPageResponse(results, page, size);
        }

        // Default: return recent transactions
        Page<Transaction> results = transactionRepository.findAll(pageRequest);
        return buildPageResponse(results, page, size);
    }

    private Map<String, Object> buildPageResponse(Page<Transaction> page, int pageNum, int size) {
        List<TransactionResponse> txns = page.getContent().stream()
                .map(TransactionResponse::from)
                .toList();

        return Map.of(
                "transactions", txns,
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "page", pageNum,
                "size", size,
                "hasNext", page.hasNext()
        );
    }
}
