-- ═══════════════════════════════════════════════════════════════════
-- V3: Modules 9-12
--   Module 9  — User Registration & KYC
--   Module 10 — Merchant Onboarding & QR Payments
--   Module 11 — Operations & Incident Response
--   Module 12 — UPI Collect & Mandate (Recurring Payments)
-- ═══════════════════════════════════════════════════════════════════

-- ─── MODULE 9: User Registration & KYC ───────────────────────────

-- SIM binding / device binding records
CREATE TABLE device_binding (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES users(id),
    mobile_number       VARCHAR(15) NOT NULL,
    device_id           VARCHAR(200) NOT NULL,
    imei                VARCHAR(20),
    sim_serial          VARCHAR(30),
    binding_sms_id      VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / SMS_SENT / VERIFIED / FAILED / EXPIRED
    bound_at            TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_device_binding_mobile ON device_binding(mobile_number);
CREATE INDEX idx_device_binding_user ON device_binding(user_id);

-- KYC records
CREATE TABLE user_kyc (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    kyc_level               VARCHAR(20) NOT NULL DEFAULT 'MINIMUM',  -- MINIMUM / FULL
    document_type           VARCHAR(30),             -- CITIZENSHIP_CERTIFICATE / PASSPORT / NATIONAL_ID
    document_number         VARCHAR(50),
    document_image_hash     VARCHAR(128),
    selfie_image_hash       VARCHAR(128),
    ocr_extracted_data      JSONB,                   -- {name, dob, id_number, address}
    face_match_score        NUMERIC(5,2),            -- 0.00 to 100.00
    verification_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / AUTO_APPROVED / MANUAL_REVIEW / APPROVED / REJECTED
    rejection_reason        TEXT,
    verified_by             VARCHAR(200),            -- SYSTEM or operator name
    verified_at             TIMESTAMPTZ,
    document_expiry         DATE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_user_kyc_user ON user_kyc(user_id);
CREATE INDEX idx_user_kyc_doc ON user_kyc(document_type, document_number);
CREATE INDEX idx_user_kyc_status ON user_kyc(verification_status);

-- Device change / account recovery requests
CREATE TABLE device_change_request (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    old_device_id       VARCHAR(200),
    new_device_id       VARCHAR(200) NOT NULL,
    new_sim_serial      VARCHAR(30),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',  -- PENDING_VERIFICATION / COOLING_PERIOD / COMPLETED / REJECTED
    mpin_verified       BOOLEAN DEFAULT false,
    cooling_ends_at     TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_device_change_user ON device_change_request(user_id);

-- Extend users table for registration fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number_verified BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS device_bound BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS kyc_level VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS daily_limit_paisa BIGINT DEFAULT 2500000;   -- Rs 25,000 default
ALTER TABLE users ADD COLUMN IF NOT EXISTS mpin_set BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_device_change_at TIMESTAMPTZ;

-- ─── MODULE 10: Merchant Onboarding & QR Payments ────────────────

CREATE TABLE merchant (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id             VARCHAR(50) UNIQUE NOT NULL,   -- "MER-TEASTALL-12345"
    user_id                 UUID REFERENCES users(id),     -- linked user account
    psp_id                  VARCHAR(50),
    business_name           VARCHAR(300) NOT NULL,
    business_category       VARCHAR(50) NOT NULL,           -- FOOD_BEVERAGE, RETAIL, TRANSPORT, etc.
    mcc_code                VARCHAR(4),                     -- ISO 18245 MCC
    merchant_type           VARCHAR(10) NOT NULL DEFAULT 'SMALL',  -- SMALL / LARGE
    merchant_vpa            VARCHAR(100) UNIQUE,
    -- Address
    address_line            VARCHAR(500),
    city                    VARCHAR(100),
    district                VARCHAR(100),
    -- Documents (large merchants)
    pan_number              VARCHAR(20),
    registration_doc_hash   VARCHAR(128),
    -- QR
    static_qr_data          TEXT,
    -- API access (large merchants)
    api_key_hash            VARCHAR(200),
    api_secret_hash         VARCHAR(200),
    webhook_url             VARCHAR(500),
    -- Settlement
    settlement_account_id   UUID REFERENCES bank_accounts(id),
    settlement_cycle        VARCHAR(10) DEFAULT 'T1',      -- T0 / T1
    mdr_percent             NUMERIC(5,4) DEFAULT 0.0000,   -- zero for small merchants
    -- Notifications
    push_enabled            BOOLEAN DEFAULT true,
    audio_notification      BOOLEAN DEFAULT true,
    -- Status
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / ACTIVE / SUSPENDED / TERMINATED
    suspended_reason        VARCHAR(500),
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_merchant_user ON merchant(user_id);
CREATE INDEX idx_merchant_type ON merchant(merchant_type);
CREATE INDEX idx_merchant_status ON merchant(status);
CREATE INDEX idx_merchant_vpa ON merchant(merchant_vpa);

-- QR code records (dynamic QR has amount + expiry)
CREATE TABLE merchant_qr_code (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID NOT NULL REFERENCES merchant(id),
    qr_type             VARCHAR(10) NOT NULL DEFAULT 'STATIC',  -- STATIC / DYNAMIC
    qr_data             TEXT NOT NULL,
    qr_payload          JSONB,                          -- encoded fields
    amount_paisa        BIGINT,                         -- null for static
    merchant_txn_ref    VARCHAR(100),                   -- unique ref for dynamic
    description         VARCHAR(500),
    expires_at          TIMESTAMPTZ,
    scanned             BOOLEAN DEFAULT false,
    transaction_id      UUID REFERENCES transactions(id),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_merchant_qr_merchant ON merchant_qr_code(merchant_id);
CREATE INDEX idx_merchant_qr_ref ON merchant_qr_code(merchant_txn_ref);

-- Daily merchant settlement records
CREATE TABLE merchant_settlement (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id             UUID NOT NULL REFERENCES merchant(id),
    settlement_date         DATE NOT NULL,
    total_txn_count         INTEGER DEFAULT 0,
    total_amount_paisa      BIGINT DEFAULT 0,
    mdr_deducted_paisa      BIGINT DEFAULT 0,
    net_amount_paisa        BIGINT DEFAULT 0,
    settlement_status       VARCHAR(20) DEFAULT 'PENDING',  -- PENDING / SETTLED / FAILED
    settlement_reference    VARCHAR(100),
    settled_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_merchant_settlement_unique ON merchant_settlement(merchant_id, settlement_date);

-- ─── MODULE 11: Operations & Incident Response ───────────────────

CREATE TABLE incident (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_number     VARCHAR(20) UNIQUE NOT NULL,    -- "INC-20260302-001"
    severity            INTEGER NOT NULL,                -- 1, 2, 3, 4
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'DETECTED',  -- DETECTED / ACKNOWLEDGED / INVESTIGATING / MITIGATING / RESOLVED / POSTMORTEM_PENDING
    -- Timing
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    duration_minutes    INTEGER,
    -- Staff
    on_call_engineer    VARCHAR(200),
    escalation_level    INTEGER DEFAULT 0,
    escalated_to        VARCHAR(200),
    -- Communication
    slack_channel       VARCHAR(100),
    nrb_notified        BOOLEAN DEFAULT false,
    nrb_notified_at     TIMESTAMPTZ,
    psp_notified        BOOLEAN DEFAULT false,
    status_page_updated BOOLEAN DEFAULT false,
    -- Resolution
    root_cause          TEXT,
    resolution_summary  TEXT,
    -- Metadata
    affected_service    VARCHAR(100),
    affected_bank_code  VARCHAR(20),
    impact_description  TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_incident_severity ON incident(severity);
CREATE INDEX idx_incident_status ON incident(status);
CREATE INDEX idx_incident_detected ON incident(detected_at);

-- Incident timeline entries (append-only log)
CREATE TABLE incident_timeline (
    id              BIGSERIAL PRIMARY KEY,
    incident_id     UUID NOT NULL REFERENCES incident(id),
    entry_type      VARCHAR(30) NOT NULL,              -- NOTE / ESCALATION / STATUS_CHANGE / NOTIFICATION / ACTION
    message         TEXT NOT NULL,
    author          VARCHAR(200),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_timeline_incident ON incident_timeline(incident_id);

-- Runbook library
CREATE TABLE runbook (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(300) NOT NULL,
    category            VARCHAR(50) NOT NULL,           -- DATABASE / MESSAGING / NETWORK / BANK / SETTLEMENT / FRAUD / API
    symptoms            TEXT NOT NULL,
    diagnostic_steps    TEXT NOT NULL,
    remediation_steps   TEXT NOT NULL,
    verification_steps  TEXT NOT NULL,
    escalation_path     TEXT,
    last_used_at        TIMESTAMPTZ,
    version             INTEGER DEFAULT 1,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_runbook_category ON runbook(category);

-- On-call schedule
CREATE TABLE on_call_schedule (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    engineer_name       VARCHAR(200) NOT NULL,
    engineer_email      VARCHAR(200) NOT NULL,
    engineer_phone      VARCHAR(20) NOT NULL,
    role                VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',  -- PRIMARY / SECONDARY
    week_start          DATE NOT NULL,
    week_end            DATE NOT NULL,
    is_active           BOOLEAN DEFAULT true,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_oncall_dates ON on_call_schedule(week_start, week_end);

-- Post-incident reviews
CREATE TABLE postmortem (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         UUID NOT NULL REFERENCES incident(id),
    title               VARCHAR(500) NOT NULL,
    timeline_summary    TEXT,
    root_cause_analysis TEXT NOT NULL,
    contributing_factors TEXT,
    what_went_well      TEXT,
    what_went_poorly    TEXT,
    action_items        JSONB,                          -- [{description, owner, due_date, status, ticket_id}]
    review_date         DATE,
    attendees           JSONB,                          -- ["name1", "name2"]
    status              VARCHAR(20) DEFAULT 'DRAFT',    -- DRAFT / REVIEWED / COMPLETED
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_postmortem_incident ON postmortem(incident_id);

-- ─── MODULE 12: UPI Collect & Mandate ────────────────────────────

-- Collect (pull) payment requests
CREATE TABLE collect_request (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collect_ref         VARCHAR(30) UNIQUE NOT NULL,    -- "COL-20260302-001"
    requestor_vpa       VARCHAR(100) NOT NULL,
    payer_vpa           VARCHAR(100) NOT NULL,
    amount_paisa        BIGINT NOT NULL,
    description         VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED / EXPIRED
    expires_at          TIMESTAMPTZ NOT NULL,
    responded_at        TIMESTAMPTZ,
    rejection_reason    VARCHAR(500),
    transaction_id      UUID REFERENCES transactions(id),       -- linked txn if approved
    requestor_psp_id    VARCHAR(50),
    payer_psp_id        VARCHAR(50),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_collect_payer ON collect_request(payer_vpa);
CREATE INDEX idx_collect_requestor ON collect_request(requestor_vpa);
CREATE INDEX idx_collect_status ON collect_request(status);
CREATE INDEX idx_collect_expires ON collect_request(expires_at);

-- Mandate (recurring payment authorization)
CREATE TABLE mandate (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mandate_ref         VARCHAR(30) UNIQUE NOT NULL,    -- "MND-20260302-001"
    merchant_vpa        VARCHAR(100) NOT NULL,
    payer_vpa           VARCHAR(100) NOT NULL,
    amount_paisa        BIGINT,                         -- fixed amount (null if variable)
    max_amount_paisa    BIGINT NOT NULL,                -- ceiling
    frequency           VARCHAR(20) NOT NULL,           -- WEEKLY / MONTHLY / QUARTERLY / YEARLY / ONE_TIME
    category            VARCHAR(30) NOT NULL,           -- SUBSCRIPTION / LOAN_EMI / INSURANCE / UTILITY / MUTUAL_FUND / OTHER
    mandate_type        VARCHAR(15) NOT NULL DEFAULT 'RECURRING',  -- RECURRING / ONE_TIME
    purpose             VARCHAR(500),
    start_date          DATE NOT NULL,
    end_date            DATE,
    next_debit_date     DATE,
    last_debit_date     DATE,
    -- Status
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',  -- PENDING_APPROVAL / ACTIVE / PAUSED / CANCELLED / EXPIRED / COMPLETED
    approved_at         TIMESTAMPTZ,
    paused_at           TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(500),
    -- One-time mandate cooling
    cooling_period_minutes INTEGER DEFAULT 0,
    cooling_ends_at     TIMESTAMPTZ,
    -- Merchant info
    merchant_psp_id     VARCHAR(50),
    payer_psp_id        VARCHAR(50),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_mandate_payer ON mandate(payer_vpa);
CREATE INDEX idx_mandate_merchant ON mandate(merchant_vpa);
CREATE INDEX idx_mandate_status ON mandate(status);
CREATE INDEX idx_mandate_next_debit ON mandate(next_debit_date);

-- Individual mandate debit execution records
CREATE TABLE mandate_execution (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mandate_id              UUID NOT NULL REFERENCES mandate(id),
    scheduled_date          DATE NOT NULL,
    amount_paisa            BIGINT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',  -- SCHEDULED / PRE_NOTIFIED / EXECUTING / COMPLETED / FAILED / RETRYING
    transaction_id          UUID REFERENCES transactions(id),
    retry_count             INTEGER DEFAULT 0,
    pre_notification_sent_at TIMESTAMPTZ,
    executed_at             TIMESTAMPTZ,
    failure_reason          VARCHAR(500),
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_mandate_exec_mandate ON mandate_execution(mandate_id);
CREATE INDEX idx_mandate_exec_date ON mandate_execution(scheduled_date);
CREATE INDEX idx_mandate_exec_status ON mandate_execution(status);
