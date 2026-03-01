# Nepal UPI Switch

A complete UPI (Unified Payments Interface) switch implementation for Nepal, modeled on India's NPCI architecture and adapted for Nepal's ecosystem (NRB + NCHL + ConnectIPS).

## Architecture

```
PSP Apps (eSewa, Khalti, Bank Apps)    ← Frontend apps users interact with
              ↓
   Nepal UPI Switch (this project)     ← Central payment infrastructure
              ↓
    Bank CBS via NCHL Switch           ← Actual money movement
```

## Tech Stack

| Component   | Technology                      |
|-------------|----------------------------------|
| Language    | Java 21                          |
| Framework   | Spring Boot 3.2                  |
| Database    | PostgreSQL 16                    |
| Cache       | Redis 7                          |
| Messaging   | Apache Kafka 3.7                 |
| Migrations  | Flyway                           |
| Security    | HMAC-SHA256 request signing      |
| Crypto      | HSM (dev: software RSA)          |
| Monitoring  | OpenTelemetry + Prometheus       |

## Project Structure

```
src/main/java/np/com/nepalupi/
├── config/              # Security, Kafka, Async configs
├── controller/          # REST API endpoints
├── domain/
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities
│   ├── enums/           # TransactionStatus, FraudSignal, etc.
│   └── event/           # Kafka event models
├── exception/           # Custom exceptions + global handler
├── filter/              # PSP authentication filter
├── repository/          # JPA repositories
├── service/
│   ├── bank/            # Bank connectors (NCHL adapter)
│   ├── fraud/           # Real-time fraud engine
│   ├── notification/    # Kafka producer/consumer for notifications
│   ├── pin/             # MPIN encryption (HSM integration)
│   ├── settlement/      # EOD settlement engine
│   ├── transaction/     # Orchestrator, state machine, limits
│   └── vpa/             # VPA resolution service
└── util/                # ID generators
```

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, and Kafka.

### 2. Run the Application

```bash
./gradlew bootRun
```

### 3. Test a Payment

```bash
# Resolve a VPA
curl -X POST http://localhost:8080/api/v1/vpa/resolve \
  -H "Content-Type: application/json" \
  -d '{"vpa": "ritesh@nchl"}'

# Initiate a payment
curl -X POST http://localhost:8080/api/v1/transactions/initiate \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -d '{
    "payerVpa": "ritesh@nchl",
    "payeeVpa": "sita@nchl",
    "amount": 150050,
    "note": "Dinner payment"
  }'

# Check transaction status
curl http://localhost:8080/api/v1/transactions/{upiTxnId}
```

## API Endpoints

| Method | Endpoint                              | Description              |
|--------|---------------------------------------|--------------------------|
| POST   | `/api/v1/vpa/resolve`                 | Resolve VPA to bank info |
| POST   | `/api/v1/transactions/initiate`       | Initiate a payment       |
| GET    | `/api/v1/transactions/{upiTxnId}`     | Check transaction status |
| POST   | `/api/v1/dispute/raise`               | Raise a dispute          |
| GET    | `/api/v1/dispute/{id}`                | Get dispute details      |
| GET    | `/health`                             | Health check             |

## Transaction State Machine

```
INITIATED → DEBIT_PENDING → DEBITED → CREDIT_PENDING → COMPLETED
                  ↓              ↓              ↓
            DEBIT_FAILED    CREDIT_FAILED → REVERSAL_PENDING
                                                  ↓
                                          REVERSED / REVERSAL_FAILED
```

All transitions enforced by `TransactionStateMachine`. Direct status updates are forbidden.

## Key Design Principles

1. **Amounts in PAISA** — `Rs 1500.50` = `150050` paisa. Never use floating point.
2. **Idempotency everywhere** — Duplicate requests return the same result, never double-process.
3. **State machine is gospel** — Only `TransactionStateMachine` can change transaction status.
4. **HSM for crypto** — PIN encryption via Hardware Security Module (dev uses software RSA).
5. **Every rupee accounted** — Nightly reconciliation validates settlement totals match transaction totals.
6. **Timeout ≠ Failure** — Bank timeouts trigger status checks, never assume failure.

## Settlement

Runs daily at 11:59 PM Nepal time. Calculates net position per bank and generates NRB-compliant settlement reports.

## NRB Compliance

- Transaction logs retained 5+ years
- End-to-end encryption (at rest + in transit)
- Disaster recovery: RPO < 4 hours, RTO < 2 hours
- Physical data residency in Nepal
- AML/KYC compliance framework
- Daily settlement reconciliation

## License

Proprietary — Nepal UPI Switch
