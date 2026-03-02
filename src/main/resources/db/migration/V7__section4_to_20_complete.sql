-- ═══════════════════════════════════════════════════════════════
-- V7: Complete UPI System — Sections 4-20 schema additions
-- ═══════════════════════════════════════════════════════════════

-- ── Section 4: VPA transfer history ──────────────────────────

-- ── Section 9.2: MPIN lock fields on users ───────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS mpin_wrong_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS mpin_locked_until TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS vpa_transfer_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpa_address     VARCHAR(100) NOT NULL,
    from_bank_code  VARCHAR(20)  NOT NULL,
    to_bank_code    VARCHAR(20)  NOT NULL,
    from_account_id UUID         NOT NULL,
    to_account_id   UUID         NOT NULL,
    transferred_by  UUID         NOT NULL,
    transferred_at  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- ── Section 6: Collect anti-spam tracking ────────────────────
CREATE TABLE IF NOT EXISTS collect_spam_tracker (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requestor_vpa   VARCHAR(100) NOT NULL,
    target_payer_vpa VARCHAR(100) NOT NULL,
    request_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    request_count   INTEGER NOT NULL DEFAULT 1,
    blocked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    UNIQUE (requestor_vpa, target_payer_vpa, request_date)
);

CREATE TABLE IF NOT EXISTS collect_block_list (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payer_vpa       VARCHAR(100) NOT NULL,
    blocked_vpa     VARCHAR(100) NOT NULL,
    reason          VARCHAR(255),
    blocked_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    UNIQUE (payer_vpa, blocked_vpa)
);

-- ── Section 7: Mandate pre-debit notification tracking ──────
CREATE TABLE IF NOT EXISTS mandate_notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mandate_id      UUID NOT NULL,
    execution_id    UUID,
    notification_type VARCHAR(30) NOT NULL, -- PRE_DEBIT, EXECUTION, EXPIRY
    sent_at         TIMESTAMP WITH TIME ZONE DEFAULT now(),
    channel         VARCHAR(20) NOT NULL DEFAULT 'PUSH', -- PUSH, SMS, EMAIL
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
    message         TEXT
);

-- ── Section 9: Security enhancements ────────────────────────
CREATE TABLE IF NOT EXISTS ip_whitelist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id          VARCHAR(50) NOT NULL,
    ip_address      VARCHAR(45) NOT NULL, -- Supports IPv6
    cidr_range      VARCHAR(50),
    description     VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    expires_at      TIMESTAMP WITH TIME ZONE,
    UNIQUE (psp_id, ip_address)
);

CREATE TABLE IF NOT EXISTS api_key_rotation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id          VARCHAR(50) NOT NULL,
    key_hash        VARCHAR(128) NOT NULL,
    key_version     INTEGER NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    rotated_at      TIMESTAMP WITH TIME ZONE,
    previous_key_id UUID
);

-- ── Section 12: OTP tracking ────────────────────────────────
CREATE TABLE IF NOT EXISTS otp_verification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mobile_number   VARCHAR(15) NOT NULL,
    otp_hash        VARCHAR(128) NOT NULL,
    purpose         VARCHAR(30) NOT NULL, -- REGISTRATION, MPIN_RESET, DEVICE_CHANGE
    attempts        INTEGER NOT NULL DEFAULT 0,
    max_attempts    INTEGER NOT NULL DEFAULT 3,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at     TIMESTAMP WITH TIME ZONE
);

-- ── Section 14: Feature flags ───────────────────────────────
CREATE TABLE IF NOT EXISTS feature_flag (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key        VARCHAR(100) NOT NULL UNIQUE,
    flag_value      BOOLEAN NOT NULL DEFAULT FALSE,
    description     VARCHAR(500),
    category        VARCHAR(50), -- TRANSACTION, SECURITY, SETTLEMENT, UPI_LITE
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_by      VARCHAR(100)
);

-- Insert default feature flags
INSERT INTO feature_flag (id, flag_key, flag_value, description, category) VALUES
    (gen_random_uuid(), 'upi.lite.enabled', false, 'Enable UPI Lite small-value payments', 'UPI_LITE'),
    (gen_random_uuid(), 'upi.international.enabled', false, 'Enable cross-border UPI payments', 'TRANSACTION'),
    (gen_random_uuid(), 'upi.credit_line.enabled', false, 'Enable UPI Credit Line (pay later)', 'TRANSACTION'),
    (gen_random_uuid(), 'upi.signed_qr.enabled', false, 'Enable digitally signed QR codes', 'SECURITY'),
    (gen_random_uuid(), 'settlement.2_hour_cycle.enabled', true, 'Enable 2-hour settlement cycles', 'SETTLEMENT'),
    (gen_random_uuid(), 'fraud.ml_scoring.enabled', false, 'Enable ML-based fraud scoring', 'SECURITY'),
    (gen_random_uuid(), 'collect.anti_spam.enabled', true, 'Enable collect request anti-spam', 'TRANSACTION'),
    (gen_random_uuid(), 'mandate.pre_debit_notification.enabled', true, 'Enable 24h pre-debit notifications', 'TRANSACTION'),
    (gen_random_uuid(), 'security.ip_whitelist.enabled', false, 'Enable PSP IP whitelisting', 'SECURITY'),
    (gen_random_uuid(), 'observability.distributed_tracing.enabled', true, 'Enable OpenTelemetry tracing', 'OBSERVABILITY')
ON CONFLICT (flag_key) DO NOTHING;

-- ── Section 17: Transaction partitioning support ─────────────
-- (Index to support date-range queries for future partitioning)
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_status_created ON transactions (status, created_at);

-- ── Section 19: UPI Lite wallet ─────────────────────────────
CREATE TABLE IF NOT EXISTS upi_lite_wallet (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    balance_paisa   BIGINT NOT NULL DEFAULT 0,
    max_balance_paisa BIGINT NOT NULL DEFAULT 200000, -- NPR 2,000
    per_txn_limit_paisa BIGINT NOT NULL DEFAULT 50000, -- NPR 500
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    linked_bank_account_id UUID NOT NULL,
    linked_bank_code VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS upi_lite_transaction (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL REFERENCES upi_lite_wallet(id),
    txn_type        VARCHAR(20) NOT NULL, -- LOAD, PAY, REFUND
    amount_paisa    BIGINT NOT NULL,
    payer_vpa       VARCHAR(100),
    payee_vpa       VARCHAR(100),
    description     VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    settled         BOOLEAN NOT NULL DEFAULT FALSE,
    settlement_batch_id VARCHAR(50),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- ── Section 19: International remittance ────────────────────
CREATE TABLE IF NOT EXISTS international_remittance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_country  VARCHAR(3) NOT NULL DEFAULT 'NPL',
    dest_country    VARCHAR(3) NOT NULL,
    source_currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    dest_currency   VARCHAR(3) NOT NULL,
    source_amount_minor BIGINT NOT NULL,
    dest_amount_minor BIGINT NOT NULL,
    exchange_rate   DECIMAL(15,6) NOT NULL,
    payer_vpa       VARCHAR(100) NOT NULL,
    payee_identifier VARCHAR(200) NOT NULL,
    partner_system  VARCHAR(50) NOT NULL, -- CONNECTIPS, IMEPAY, etc.
    status          VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    compliance_check_status VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    completed_at    TIMESTAMP WITH TIME ZONE
);

-- ── Section 16: Dead letter queue tracking ──────────────────
CREATE TABLE IF NOT EXISTS dead_letter_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           VARCHAR(100) NOT NULL,
    event_key       VARCHAR(200),
    event_payload   TEXT NOT NULL,
    error_message   TEXT,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    max_retries     INTEGER NOT NULL DEFAULT 5,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RETRYING, RESOLVED, ABANDONED
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    last_retry_at   TIMESTAMP WITH TIME ZONE,
    resolved_at     TIMESTAMP WITH TIME ZONE
);

-- ── Section 17: Transaction archival tracking ───────────────
CREATE TABLE IF NOT EXISTS transaction_archive_batch (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    archive_date    DATE NOT NULL,
    txn_count       BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    archive_location VARCHAR(500), -- S3/file path
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    completed_at    TIMESTAMP WITH TIME ZONE
);
