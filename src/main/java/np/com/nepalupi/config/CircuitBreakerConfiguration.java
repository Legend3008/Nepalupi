package np.com.nepalupi.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Section 16: Circuit breaker configuration per bank.
 * <p>
 * If bank failure rate > 50% in 60s → circuit OPEN → reject new requests.
 * Half-open after 30s → allow probe requests.
 * Close circuit when success rate recovers.
 */
@Configuration
@Slf4j
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)                   // 50% failure → OPEN
                .slowCallRateThreshold(80.0f)                  // 80% slow → OPEN
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .waitDurationInOpenState(Duration.ofSeconds(30)) // 30s before half-open
                .permittedNumberOfCallsInHalfOpenState(3)       // 3 probe requests
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(60)                          // 60 second window
                .minimumNumberOfCalls(10)                       // min 10 calls before evaluating
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        
        // Pre-register circuit breakers for known Nepal banks
        String[] banks = {"NCHL", "NABIL", "GLOBAL", "NIC", "SANIMA", "MEGA", "NMB", "SUNRISE", "LAXMI", "SBI"};
        for (String bank : banks) {
            CircuitBreaker cb = registry.circuitBreaker("bank-" + bank);
            cb.getEventPublisher()
                    .onStateTransition(event -> log.warn("Circuit breaker state change: bank={}, from={} to={}",
                            bank, event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                    .onCallNotPermitted(event -> log.error("Circuit OPEN — rejecting call to bank: {}", bank));
        }

        return registry;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))  // 10s bank call timeout
                .cancelRunningFuture(true)
                .build();
        return TimeLimiterRegistry.of(defaultConfig);
    }
}
