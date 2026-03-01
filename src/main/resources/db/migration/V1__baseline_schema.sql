-- ═══════════════════════════════════════════════════════════════
-- Nepal UPI — V1 Baseline Schema
-- All monetary values stored in PAISA (1 NPR = 100 paisa)
-- ═══════════════════════════════════════════════════════════════

-- ── Extension for UUID generation ────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── PSP (Payment Service Provider) ──────────────────────────
CREATE TABLE psp (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id        VARCHAR(50) UNIQUE NOT NULL,
    name          VARCHAR(200) NOT NULL,
    api_key_hash  VARCHAR(200) NOT NULL,
    secret_hash   VARCHAR(200) NOT NULL,
    webhook_url   VARCHAR(500),
    is_active     BOOLEAN DEFAULT true,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ── Users ────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mobile_number   VARCHAR(15) UNIQUE NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    kyc_status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    device_id       VARCHAR(200),
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_mobile ON users(mobile_number);

-- ── Bank Accounts ────────────────────────────────────────────
CREATE TABLE bank_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    bank_code       VARCHAR(20) NOT NULL,
    account_number  VARCHAR(50) NOT NULL,
    account_holder  VARCHAR(200) NOT NULL,
    account_type    VARCHAR(20) DEFAULT 'SAVINGS',
    is_verified     BOOLEAN DEFAULT false,
    is_primary      BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(bank_code, account_number)
);

CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id);

-- ── VPA (Virtual Payment Address) ────────────────────────────
CREATE TABLE vpa (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpa_address     VARCHAR(100) UNIQUE NOT NULL,   -- e.g. ritesh@nchl
    user_id         UUID NOT NULL REFERENCES users(id),
    bank_account_id UUID NOT NULL REFERENCES bank_accounts(id),
    bank_code       VARCHAR(20) NOT NULL,
    is_primary      BOOLEAN DEFAULT false,
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_vpa_user ON vpa(user_id);
CREATE INDEX idx_vpa_address ON vpa(vpa_address);

-- ── Transactions ─────────────────────────────────────────────
CREATE TABLE transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upi_txn_id        VARCHAR(50) UNIQUE NOT NULL,
    rrn               VARCHAR(50) UNIQUE NOT NULL,

    -- Transaction type
    txn_type          VARCHAR(30) NOT NULL DEFAULT 'PAY',

    -- Parties
    payer_vpa         VARCHAR(100) NOT NULL,
    payee_vpa         VARCHAR(100) NOT NULL,
    payer_bank_code   VARCHAR(20) NOT NULL,
    payee_bank_code   VARCHAR(20) NOT NULL,

    -- Money (always in PAISA — never decimal)
    amount            BIGINT NOT NULL CHECK (amount > 0),
    currency          VARCHAR(3) DEFAULT 'NPR',

    -- State machine
    status            VARCHAR(30) NOT NULL,
    failure_reason    VARCHAR(500),
    failure_code      VARCHAR(30),

    -- Timing
    initiated_at      TIMESTAMPTZ DEFAULT NOW(),
    debited_at        TIMESTAMPTZ,
    credited_at       TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    expires_at        TIMESTAMPTZ NOT NULL,

    -- Audit trail
    psp_id            VARCHAR(50),
    device_fingerprint TEXT,
    ip_address        INET,
    note              VARCHAR(200),

    -- Idempotency
    idempotency_key   VARCHAR(100) UNIQUE NOT NULL,

    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_txn_payer_vpa       ON transactions(payer_vpa);
CREATE INDEX idx_txn_payee_vpa       ON transactions(payee_vpa);
CREATE INDEX idx_txn_status          ON transactions(status);
CREATE INDEX idx_txn_initiated_at    ON transactions(initiated_at);
CREATE INDEX idx_txn_payer_bank      ON transactions(payer_bank_code);
CREATE INDEX idx_txn_payee_bank      ON transactions(payee_bank_code);
CREATE INDEX idx_txn_upi_txn_id     ON transactions(upi_txn_id);
CREATE INDEX idx_txn_idempotency    ON transactions(idempotency_key);

-- ── Transaction Audit Log ────────────────────────────────────
CREATE TABLE transaction_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_at      TIMESTAMPTZ DEFAULT NOW(),
    metadata        JSONB
);

CREATE INDEX idx_audit_txn ON transaction_audit_log(transaction_id);

-- ── Daily Transaction Stats (for limit tracking) ────────────
CREATE TABLE daily_transaction_stats (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    stats_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    total_amount_paisa  BIGINT NOT NULL DEFAULT 0,
    transaction_count   INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, stats_date)
);

CREATE INDEX idx_daily_stats_user_date ON daily_transaction_stats(user_id, stats_date);

-- ── Settlement Reports ───────────────────────────────────────
CREATE TABLE settlement_reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_date     DATE UNIQUE NOT NULL,
    total_transactions  INTEGER NOT NULL,
    total_volume_paisa  BIGINT NOT NULL,
    net_positions       JSONB NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    submitted_at        TIMESTAMPTZ,
    confirmed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Disputes ─────────────────────────────────────────────────
CREATE TABLE disputes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    raised_by_vpa   VARCHAR(100) NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution      VARCHAR(500),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_disputes_txn ON disputes(transaction_id);

-- ── Fraud Flags ──────────────────────────────────────────────
CREATE TABLE fraud_flags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    transaction_id  UUID REFERENCES transactions(id),
    signals         JSONB NOT NULL,
    reviewed        BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_fraud_user ON fraud_flags(user_id);
