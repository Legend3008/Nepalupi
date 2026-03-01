-- ═══════════════════════════════════════════════════════════════
-- V2: Module 1-4 Schema — ISO 8583, Disputes, PSP Onboarding,
--     NRB Compliance Reporting
-- ═══════════════════════════════════════════════════════════════

-- ── MODULE 1: NCHL ISO 8583 ─────────────────────────────────

-- ISO 8583 message log — every message to/from NCHL
CREATE TABLE iso8583_message_log (
    id              BIGSERIAL PRIMARY KEY,
    mti             VARCHAR(4) NOT NULL,           -- 0200, 0210, 0400, 0800, etc.
    direction       VARCHAR(10) NOT NULL,          -- OUTBOUND / INBOUND
    bitmap          VARCHAR(64),                    -- hex-encoded bitmap
    processing_code VARCHAR(6),                     -- field 3
    amount_paisa    BIGINT,                         -- field 4
    stan            VARCHAR(6),                     -- field 11
    rrn             VARCHAR(12),                    -- field 37
    response_code   VARCHAR(2),                     -- field 39
    auth_code       VARCHAR(6),                     -- field 38
    currency_code   VARCHAR(3) DEFAULT '524',       -- field 49 (524 = NPR)
    raw_hex         TEXT,                           -- full raw message hex dump
    transaction_id  UUID REFERENCES transactions(id),
    channel         VARCHAR(20) DEFAULT 'PRIMARY',  -- PRIMARY / DR
    sent_at         TIMESTAMPTZ,
    received_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_iso_msg_rrn ON iso8583_message_log(rrn);
CREATE INDEX idx_iso_msg_stan ON iso8583_message_log(stan);
CREATE INDEX idx_iso_msg_txn ON iso8583_message_log(transaction_id);

-- NCHL connection state tracking
CREATE TABLE nchl_connection_state (
    id              BIGSERIAL PRIMARY KEY,
    channel         VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    status          VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED',  -- CONNECTED/DISCONNECTED/SIGNED_ON/SIGNING_ON
    last_heartbeat  TIMESTAMPTZ,
    last_sign_on    TIMESTAMPTZ,
    sign_on_count   INTEGER DEFAULT 0,
    heartbeat_count INTEGER DEFAULT 0,
    fail_count      INTEGER DEFAULT 0,
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO nchl_connection_state (channel, status) VALUES ('PRIMARY', 'DISCONNECTED');
INSERT INTO nchl_connection_state (channel, status) VALUES ('DR', 'DISCONNECTED');


-- ── MODULE 2: DISPUTE RESOLUTION (Enhanced) ─────────────────

-- Dispute action log — every action on a dispute
CREATE TABLE dispute_action_log (
    id              BIGSERIAL PRIMARY KEY,
    dispute_id      UUID NOT NULL REFERENCES disputes(id),
    action          VARCHAR(50) NOT NULL,            -- RAISED, ACKNOWLEDGED, BANK_QUERY_SENT, BANK_RESPONSE, AUTO_RESOLVED, MANUAL_ESCALATED, RESOLVED, CLOSED
    performed_by    VARCHAR(200),                     -- system / ops agent name
    details         TEXT,
    attachment_url  VARCHAR(500),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_dispute_action_dispute ON dispute_action_log(dispute_id);

-- Add new columns to existing disputes table
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS dispute_type VARCHAR(50) DEFAULT 'DEBIT_WITHOUT_CREDIT';
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS case_ref VARCHAR(30) UNIQUE;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMPTZ;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS payer_bank_code VARCHAR(20);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS payee_bank_code VARCHAR(20);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS amount_paisa BIGINT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS refund_txn_id UUID REFERENCES transactions(id);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS auto_resolved BOOLEAN DEFAULT false;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS escalation_level INTEGER DEFAULT 0;   -- 0=none, 1=NCHL, 2=NRB
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS bank_query_sent_at TIMESTAMPTZ;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS bank_response_at TIMESTAMPTZ;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS bank_response_details TEXT;

CREATE INDEX idx_disputes_case_ref ON disputes(case_ref);
CREATE INDEX idx_disputes_type ON disputes(dispute_type);
CREATE INDEX idx_disputes_sla ON disputes(sla_deadline);


-- ── MODULE 3: PSP ONBOARDING ────────────────────────────────

-- Enhanced PSP table — add onboarding fields
ALTER TABLE psp ADD COLUMN IF NOT EXISTS nrb_license_number VARCHAR(50);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS nrb_license_expiry DATE;
ALTER TABLE psp ADD COLUMN IF NOT EXISTS onboarding_stage VARCHAR(30) DEFAULT 'APPLICATION';
-- APPLICATION / LEGAL_AGREEMENT / TECHNICAL_CERTIFICATION / SECURITY_REVIEW / PILOT / PRODUCTION
ALTER TABLE psp ADD COLUMN IF NOT EXISTS tier INTEGER DEFAULT 1;                   -- 1=starter, 2=standard, 3=premium
ALTER TABLE psp ADD COLUMN IF NOT EXISTS per_txn_limit_paisa BIGINT DEFAULT 1000000;     -- Rs 10,000 pilot
ALTER TABLE psp ADD COLUMN IF NOT EXISTS daily_limit_paisa BIGINT DEFAULT 10000000;      -- Rs 1,00,000 pilot
ALTER TABLE psp ADD COLUMN IF NOT EXISTS pilot_start_date DATE;
ALTER TABLE psp ADD COLUMN IF NOT EXISTS production_date DATE;
ALTER TABLE psp ADD COLUMN IF NOT EXISTS technical_contact_email VARCHAR(200);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS technical_contact_phone VARCHAR(20);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS client_cert_fingerprint VARCHAR(200);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS sandbox_token VARCHAR(200);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS webhook_signing_secret VARCHAR(200);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(500);
ALTER TABLE psp ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;

-- PSP certification test results
CREATE TABLE psp_certification_result (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id          VARCHAR(50) NOT NULL,
    test_suite      VARCHAR(50) NOT NULL,             -- VPA_RESOLUTION / PAYMENT_INITIATION / PIN_FLOW / STATUS_QUERY / WEBHOOK / ERROR_HANDLING
    test_case       VARCHAR(200) NOT NULL,
    mandatory       BOOLEAN DEFAULT true,
    passed          BOOLEAN DEFAULT false,
    details         TEXT,
    executed_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_psp_cert_psp ON psp_certification_result(psp_id);

-- PSP onboarding event log
CREATE TABLE psp_onboarding_log (
    id              BIGSERIAL PRIMARY KEY,
    psp_id          VARCHAR(50) NOT NULL,
    from_stage      VARCHAR(30),
    to_stage        VARCHAR(30) NOT NULL,
    performed_by    VARCHAR(200),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_psp_onboard_psp ON psp_onboarding_log(psp_id);

-- PSP monthly health reports
CREATE TABLE psp_health_report (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id              VARCHAR(50) NOT NULL,
    report_month        DATE NOT NULL,                  -- first day of the month
    total_transactions  INTEGER DEFAULT 0,
    successful_txns     INTEGER DEFAULT 0,
    failed_txns         INTEGER DEFAULT 0,
    success_rate        NUMERIC(5,2),
    total_volume_paisa  BIGINT DEFAULT 0,
    avg_response_ms     INTEGER,
    settlement_breaks   INTEGER DEFAULT 0,
    fraud_flags         INTEGER DEFAULT 0,
    generated_at        TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(psp_id, report_month)
);


-- ── MODULE 4: NRB COMPLIANCE REPORTING ──────────────────────

-- Immutable audit event log (append-only source of truth for compliance)
CREATE TABLE compliance_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(50) NOT NULL,              -- TXN_CREATED / TXN_STATE_CHANGE / DISPUTE_RAISED / STR_FILED / PSP_ONBOARDED / SETTLEMENT_GENERATED
    entity_type     VARCHAR(30) NOT NULL,              -- TRANSACTION / DISPUTE / PSP / SETTLEMENT / USER / VPA
    entity_id       VARCHAR(100) NOT NULL,
    details         JSONB NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_compliance_event ON compliance_audit_log(event_type);
CREATE INDEX idx_compliance_entity ON compliance_audit_log(entity_type, entity_id);
CREATE INDEX idx_compliance_created ON compliance_audit_log(created_at);

-- NRB daily reports
CREATE TABLE nrb_daily_report (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_date             DATE UNIQUE NOT NULL,
    total_txn_count         INTEGER NOT NULL DEFAULT 0,
    total_txn_value_paisa   BIGINT NOT NULL DEFAULT 0,
    p2p_count               INTEGER DEFAULT 0,
    p2p_value_paisa         BIGINT DEFAULT 0,
    p2m_count               INTEGER DEFAULT 0,        -- placeholder for future
    p2m_value_paisa         BIGINT DEFAULT 0,
    collect_count           INTEGER DEFAULT 0,
    collect_value_paisa     BIGINT DEFAULT 0,
    success_count           INTEGER DEFAULT 0,
    failure_count           INTEGER DEFAULT 0,
    failure_reasons         JSONB,                      -- {"INSUFFICIENT_FUNDS": 12, "TIMEOUT": 3, ...}
    reversal_count          INTEGER DEFAULT 0,
    reversal_value_paisa    BIGINT DEFAULT 0,
    net_settlement_position JSONB,                      -- per-bank net position
    submitted               BOOLEAN DEFAULT false,
    submitted_at            TIMESTAMPTZ,
    generated_at            TIMESTAMPTZ DEFAULT NOW()
);

-- NRB monthly volume report
CREATE TABLE nrb_monthly_report (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_month            DATE UNIQUE NOT NULL,
    total_txn_count         INTEGER NOT NULL DEFAULT 0,
    total_txn_value_paisa   BIGINT NOT NULL DEFAULT 0,
    registered_vpa_count    INTEGER DEFAULT 0,
    active_psp_count        INTEGER DEFAULT 0,
    active_user_count       INTEGER DEFAULT 0,
    new_vpa_registrations   INTEGER DEFAULT 0,
    generated_at            TIMESTAMPTZ DEFAULT NOW()
);

-- NRB quarterly risk report
CREATE TABLE nrb_quarterly_report (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_quarter          VARCHAR(10) NOT NULL UNIQUE,  -- e.g. "2026-Q1"
    fraud_incident_count    INTEGER DEFAULT 0,
    fraud_total_value_paisa BIGINT DEFAULT 0,
    fraud_types             JSONB,
    fraud_resolution_summary JSONB,
    system_downtime_minutes INTEGER DEFAULT 0,
    downtime_incidents      JSONB,                        -- [{start, end, cause, resolution}]
    security_incidents      INTEGER DEFAULT 0,
    str_filed_count         INTEGER DEFAULT 0,
    generated_at            TIMESTAMPTZ DEFAULT NOW()
);

-- Suspicious Transaction Reports (STR)
CREATE TABLE suspicious_transaction_report (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID REFERENCES transactions(id),
    user_id             UUID REFERENCES users(id),
    suspicion_type      VARCHAR(50) NOT NULL,          -- STRUCTURING / LAYERING / VELOCITY / CIRCULAR / SANCTIONS_HIT
    description         TEXT NOT NULL,
    signals             JSONB NOT NULL,
    compliance_officer  VARCHAR(200),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',  -- PENDING_REVIEW / CLEARED / FILED / ESCALATED
    filed_with_fiu      BOOLEAN DEFAULT false,
    fiu_reference       VARCHAR(100),
    filed_at            TIMESTAMPTZ,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_str_status ON suspicious_transaction_report(status);
CREATE INDEX idx_str_user ON suspicious_transaction_report(user_id);
CREATE INDEX idx_str_type ON suspicious_transaction_report(suspicion_type);

-- Sanctions list screening records
CREATE TABLE sanctions_screening (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    screened_against VARCHAR(50) NOT NULL,             -- UN_SECURITY_COUNCIL / INTERPOL / NEPAL_DOMESTIC
    match_found     BOOLEAN DEFAULT false,
    match_details   TEXT,
    screened_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sanctions_user ON sanctions_screening(user_id);
