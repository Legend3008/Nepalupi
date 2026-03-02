package np.com.nepalupi.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Section 17: Load Testing for Nepal UPI (NPI) Payment Switch.
 * <p>
 * Performance targets:
 * - 1000 TPS sustained for 10 minutes
 * - P99 latency < 5 seconds
 * - Success rate ≥ 99.5%
 * - Zero data loss under load
 * <p>
 * Test Scenarios:
 * 1. P2P Payment (Pay) — 40% of traffic
 * 2. Balance Inquiry — 30% of traffic
 * 3. Collect Request — 15% of traffic
 * 4. Transaction Status — 10% of traffic
 * 5. VPA Lookup — 5% of traffic
 */
public class NpiLoadTestSimulation extends Simulation {

    // ── Configuration ────────────────────────────────────
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8081");
    private static final int TARGET_TPS = Integer.parseInt(System.getProperty("targetTps", "1000"));
    private static final int RAMP_DURATION_SEC = Integer.parseInt(System.getProperty("rampDuration", "60"));
    private static final int SUSTAINED_DURATION_SEC = Integer.parseInt(System.getProperty("sustainedDuration", "600"));
    private static final int COOLDOWN_SEC = Integer.parseInt(System.getProperty("cooldownDuration", "30"));

    // ── HTTP Protocol ────────────────────────────────────
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("NPI-LoadTest/1.0")
            .header("X-Request-ID", session -> UUID.randomUUID().toString())
            .shareConnections();

    // ── Data Feeders ─────────────────────────────────────
    private static final String[] PAYER_VPAS = {
            "ram@nchl", "sita@nabil", "hari@global", "gita@nic", "krishna@sanima",
            "laxmi@mega", "shyam@prabhu", "radha@machhapuchhre", "gopal@kumari", "mina@siddhartha"
    };

    private static final String[] PAYEE_VPAS = {
            "shop1@nchl", "mart@nabil", "store@global", "vendor@nic", "merchant@sanima",
            "cafe@mega", "hotel@prabhu", "resto@machhapuchhre", "pharmacy@kumari", "grocer@siddhartha"
    };

    private final Iterator<Map<String, Object>> paymentFeeder = Stream.generate(
            (Supplier<Map<String, Object>>) () -> {
                Random rnd = new Random();
                return Map.of(
                        "payerVpa", PAYER_VPAS[rnd.nextInt(PAYER_VPAS.length)],
                        "payeeVpa", PAYEE_VPAS[rnd.nextInt(PAYEE_VPAS.length)],
                        "amount", (rnd.nextInt(9900) + 100),  // NPR 100 - 10,000
                        "txnId", UUID.randomUUID().toString(),
                        "remarks", "Load test payment " + rnd.nextInt(10000)
                );
            }).iterator();

    // ── Scenarios ────────────────────────────────────────

    /**
     * Scenario 1: P2P Payment (40% of traffic)
     */
    private final ScenarioBuilder p2pPayment = scenario("P2P Payment")
            .feed(paymentFeeder)
            .exec(
                    http("Pay Request")
                            .post("/api/v1/transactions/pay")
                            .body(StringBody("""
                                    {
                                        "payerVpa": "#{payerVpa}",
                                        "payeeVpa": "#{payeeVpa}",
                                        "amount": #{amount},
                                        "currency": "NPR",
                                        "remarks": "#{remarks}",
                                        "transactionType": "PAY"
                                    }
                                    """))
                            .check(status().in(200, 201, 202))
                            .check(jsonPath("$.transactionId").saveAs("txnId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500));

    /**
     * Scenario 2: Balance Inquiry (30% of traffic)
     */
    private final ScenarioBuilder balanceInquiry = scenario("Balance Inquiry")
            .feed(paymentFeeder)
            .exec(
                    http("Balance Check")
                            .get("/api/v1/accounts/#{payerVpa}/balance")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(50), Duration.ofMillis(200));

    /**
     * Scenario 3: Collect Request (15% of traffic)
     */
    private final ScenarioBuilder collectRequest = scenario("Collect Request")
            .feed(paymentFeeder)
            .exec(
                    http("Collect Request")
                            .post("/api/v1/transactions/collect")
                            .body(StringBody("""
                                    {
                                        "payerVpa": "#{payerVpa}",
                                        "payeeVpa": "#{payeeVpa}",
                                        "amount": #{amount},
                                        "currency": "NPR",
                                        "remarks": "Collect: #{remarks}",
                                        "expiryMinutes": 30
                                    }
                                    """))
                            .check(status().in(200, 201, 202))
            )
            .pause(Duration.ofMillis(200), Duration.ofMillis(800));

    /**
     * Scenario 4: Transaction Status (10% of traffic)
     */
    private final ScenarioBuilder txnStatus = scenario("Transaction Status")
            .feed(paymentFeeder)
            .exec(
                    http("Transaction Status")
                            .get("/api/v1/transactions/#{txnId}/status")
                            .check(status().in(200, 404))
            )
            .pause(Duration.ofMillis(50), Duration.ofMillis(150));

    /**
     * Scenario 5: VPA Lookup (5% of traffic)
     */
    private final ScenarioBuilder vpaLookup = scenario("VPA Lookup")
            .feed(paymentFeeder)
            .exec(
                    http("VPA Lookup")
                            .get("/api/v1/vpa/#{payeeVpa}/validate")
                            .check(status().in(200, 404))
            )
            .pause(Duration.ofMillis(50), Duration.ofMillis(100));

    // ── Load Profile ────────────────────────────────────
    {
        setUp(
                // P2P Payment: 40% of target TPS
                p2pPayment.injectOpen(
                        rampUsersPerSec(1).to(TARGET_TPS * 0.4).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
                        constantUsersPerSec(TARGET_TPS * 0.4).during(Duration.ofSeconds(SUSTAINED_DURATION_SEC)),
                        rampUsersPerSec(TARGET_TPS * 0.4).to(0).during(Duration.ofSeconds(COOLDOWN_SEC))
                ),
                // Balance Inquiry: 30% of target TPS
                balanceInquiry.injectOpen(
                        rampUsersPerSec(1).to(TARGET_TPS * 0.3).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
                        constantUsersPerSec(TARGET_TPS * 0.3).during(Duration.ofSeconds(SUSTAINED_DURATION_SEC)),
                        rampUsersPerSec(TARGET_TPS * 0.3).to(0).during(Duration.ofSeconds(COOLDOWN_SEC))
                ),
                // Collect Request: 15% of target TPS
                collectRequest.injectOpen(
                        rampUsersPerSec(1).to(TARGET_TPS * 0.15).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
                        constantUsersPerSec(TARGET_TPS * 0.15).during(Duration.ofSeconds(SUSTAINED_DURATION_SEC)),
                        rampUsersPerSec(TARGET_TPS * 0.15).to(0).during(Duration.ofSeconds(COOLDOWN_SEC))
                ),
                // Transaction Status: 10% of target TPS
                txnStatus.injectOpen(
                        rampUsersPerSec(1).to(TARGET_TPS * 0.1).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
                        constantUsersPerSec(TARGET_TPS * 0.1).during(Duration.ofSeconds(SUSTAINED_DURATION_SEC)),
                        rampUsersPerSec(TARGET_TPS * 0.1).to(0).during(Duration.ofSeconds(COOLDOWN_SEC))
                ),
                // VPA Lookup: 5% of target TPS
                vpaLookup.injectOpen(
                        rampUsersPerSec(1).to(TARGET_TPS * 0.05).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
                        constantUsersPerSec(TARGET_TPS * 0.05).during(Duration.ofSeconds(SUSTAINED_DURATION_SEC)),
                        rampUsersPerSec(TARGET_TPS * 0.05).to(0).during(Duration.ofSeconds(COOLDOWN_SEC))
                )
        ).protocols(httpProtocol)
                .assertions(
                        // SLO Assertions
                        global().responseTime().percentile4().lt(5000),     // P99 < 5s
                        global().successfulRequests().percent().gt(99.0),    // > 99% success
                        global().requestsPerSec().gt((double) (TARGET_TPS * 0.8))  // At least 80% of target TPS
                );
    }
}
