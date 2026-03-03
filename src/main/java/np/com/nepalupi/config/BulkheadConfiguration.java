package np.com.nepalupi.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Section 16.6: Bulkhead Pattern Configuration.
 * <p>
 * Isolates concurrent resources per bank/service to prevent a single
 * slow bank from consuming all thread pool resources:
 * <ul>
 *   <li>Per-bank bulkhead: max 50 concurrent calls per bank</li>
 *   <li>NCHL settlement: max 20 concurrent calls</li>
 *   <li>Balance enquiry: max 30 concurrent calls</li>
 *   <li>International remittance: max 10 concurrent calls</li>
 *   <li>Default: max 25 concurrent calls for unregistered banks</li>
 * </ul>
 * When a bulkhead is full, requests are rejected immediately with a
 * BulkheadFullException, preventing cascade failures.
 */
@Configuration
@Slf4j
public class BulkheadConfiguration {

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        // Default bulkhead config
        BulkheadConfig defaultConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(25)
                .maxWaitDuration(Duration.ofMillis(500))
                .writableStackTraceEnabled(true)
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(defaultConfig);

        // Per-bank bulkheads with higher concurrency
        BulkheadConfig bankConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(50)
                .maxWaitDuration(Duration.ofSeconds(1))
                .build();

        String[] banks = {"NCHL", "NABIL", "GLOBAL", "NIC", "SANIMA", "MEGA", "NMB", "SUNRISE", "LAXMI", "SBI"};
        for (String bank : banks) {
            Bulkhead bh = registry.bulkhead("bank-" + bank, bankConfig);
            bh.getEventPublisher()
                    .onCallPermitted(event -> log.debug("Bulkhead call permitted: bank={}", bank))
                    .onCallRejected(event -> log.warn("Bulkhead FULL — rejecting call to bank: {}", bank))
                    .onCallFinished(event -> log.debug("Bulkhead call finished: bank={}", bank));
        }

        // NCHL settlement — limited concurrency
        BulkheadConfig nchlConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(20)
                .maxWaitDuration(Duration.ofSeconds(2))
                .build();
        Bulkhead nchlBulkhead = registry.bulkhead("nchl-settlement", nchlConfig);
        nchlBulkhead.getEventPublisher()
                .onCallRejected(event -> log.error("NCHL settlement bulkhead FULL — rejecting"));

        // Balance enquiry — moderate concurrency
        BulkheadConfig balanceConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(30)
                .maxWaitDuration(Duration.ofMillis(500))
                .build();
        registry.bulkhead("balance-enquiry", balanceConfig);

        // International remittance — restricted concurrency
        BulkheadConfig internationalConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofSeconds(1))
                .build();
        registry.bulkhead("international-remittance", internationalConfig);

        // Fraud engine — critical path, dedicated pool
        BulkheadConfig fraudConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(40)
                .maxWaitDuration(Duration.ofMillis(200))
                .build();
        registry.bulkhead("fraud-engine", fraudConfig);

        log.info("Bulkhead registry initialized with {} bulkheads", 
                banks.length + 4); // banks + nchl + balance + international + fraud

        return registry;
    }
}
