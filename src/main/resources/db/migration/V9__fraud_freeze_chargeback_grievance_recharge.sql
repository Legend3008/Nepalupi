-- V9: Add missing fields and tables for remaining BHIM UPI features
-- Fraud freeze, chargeback, grievance, recharge, NFC, IVR, credit-on-UPI, soundbox, app lock, i18n, email, FCM

-- ── 1. User entity new fields ──
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_frozen BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS freeze_reason VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS app_lock_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS app_lock_type VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT 'ne';
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(500);

-- ── 1b. BankAccount entity new fields ──
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- ── 2. Chargeback table ──
CREATE TABLE IF NOT EXISTS chargebacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    transaction_id UUID NOT NULL,
    original_amount BIGINT NOT NULL,
    chargeback_amount BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'NPR',
    status VARCHAR(30) DEFAULT 'INITIATED',
    reason VARCHAR(500),
    initiated_by VARCHAR(50),
    response_from_acquirer VARCHAR(1000),
    representment_evidence TEXT,
    arbitration_status VARCHAR(30),
    sla_deadline TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ── 3. Grievance table ──
CREATE TABLE IF NOT EXISTS grievances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'OPEN',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    assigned_to VARCHAR(100),
    resolution TEXT,
    escalation_level INT DEFAULT 0,
    sla_deadline TIMESTAMP,
    resolved_at TIMESTAMP,
    ticket_number VARCHAR(30) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ── 4. Mobile recharge table ──
CREATE TABLE IF NOT EXISTS mobile_recharges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    mobile_number VARCHAR(15) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    amount_paisa BIGINT NOT NULL,
    recharge_type VARCHAR(20) DEFAULT 'PREPAID',
    plan_id VARCHAR(50),
    plan_description VARCHAR(255),
    status VARCHAR(30) DEFAULT 'INITIATED',
    transaction_id UUID,
    operator_txn_ref VARCHAR(100),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ── 5. NFC payment sessions ──
CREATE TABLE IF NOT EXISTS nfc_payment_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    merchant_vpa VARCHAR(100),
    amount_paisa BIGINT NOT NULL,
    nfc_tag_data TEXT,
    card_emulation_mode VARCHAR(20),
    terminal_id VARCHAR(50),
    status VARCHAR(30) DEFAULT 'INITIATED',
    transaction_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── 6. IVR payment sessions (123Pay) ──
CREATE TABLE IF NOT EXISTS ivr_payment_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_mobile VARCHAR(15) NOT NULL,
    callee_vpa VARCHAR(100),
    amount_paisa BIGINT NOT NULL,
    ivr_session_id VARCHAR(100),
    dtmf_input VARCHAR(50),
    status VARCHAR(30) DEFAULT 'INITIATED',
    transaction_id UUID,
    language VARCHAR(10) DEFAULT 'ne',
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── 7. Credit-on-UPI linked cards ──
CREATE TABLE IF NOT EXISTS credit_on_upi_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    card_issuer VARCHAR(100) NOT NULL,
    card_last_four VARCHAR(4) NOT NULL,
    card_network VARCHAR(20),
    linked_vpa VARCHAR(100),
    credit_limit_paisa BIGINT,
    available_limit_paisa BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    linked_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── 8. Soundbox devices ──
CREATE TABLE IF NOT EXISTS soundbox_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    device_serial VARCHAR(100) UNIQUE NOT NULL,
    device_model VARCHAR(50),
    firmware_version VARCHAR(20),
    sim_number VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    last_heartbeat_at TIMESTAMP,
    language VARCHAR(10) DEFAULT 'ne',
    volume_level INT DEFAULT 80,
    registered_at TIMESTAMP DEFAULT NOW()
);

-- ── 9. Email notification log ──
CREATE TABLE IF NOT EXISTS email_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    template_name VARCHAR(50),
    status VARCHAR(20) DEFAULT 'QUEUED',
    sent_at TIMESTAMP,
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── 10. Push notification log ──
CREATE TABLE IF NOT EXISTS push_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    fcm_token VARCHAR(500),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data_payload JSONB,
    status VARCHAR(20) DEFAULT 'QUEUED',
    sent_at TIMESTAMP,
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── 11. Spending insights cache ──
CREATE TABLE IF NOT EXISTS spending_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    period_type VARCHAR(20) NOT NULL,
    period_value VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    total_spent_paisa BIGINT DEFAULT 0,
    total_received_paisa BIGINT DEFAULT 0,
    transaction_count INT DEFAULT 0,
    avg_transaction_paisa BIGINT DEFAULT 0,
    top_payee_vpa VARCHAR(100),
    computed_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, period_type, period_value, category)
);

-- ── 12. Beneficiaries table (if not exists from JPA auto) ──
CREATE TABLE IF NOT EXISTS beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    beneficiary_vpa VARCHAR(100) NOT NULL,
    beneficiary_name VARCHAR(100),
    beneficiary_mobile VARCHAR(15),
    nick_name VARCHAR(50),
    bank_code VARCHAR(20),
    account_number VARCHAR(30),
    is_favorite BOOLEAN DEFAULT FALSE,
    last_paid_at TIMESTAMP,
    transaction_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, beneficiary_vpa)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_chargebacks_dispute ON chargebacks(dispute_id);
CREATE INDEX IF NOT EXISTS idx_chargebacks_status ON chargebacks(status);
CREATE INDEX IF NOT EXISTS idx_grievances_user ON grievances(user_id);
CREATE INDEX IF NOT EXISTS idx_grievances_status ON grievances(status);
CREATE INDEX IF NOT EXISTS idx_recharges_user ON mobile_recharges(user_id);
CREATE INDEX IF NOT EXISTS idx_nfc_sessions_user ON nfc_payment_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_ivr_sessions_mobile ON ivr_payment_sessions(caller_mobile);
CREATE INDEX IF NOT EXISTS idx_credit_cards_user ON credit_on_upi_cards(user_id);
CREATE INDEX IF NOT EXISTS idx_soundbox_merchant ON soundbox_devices(merchant_id);
CREATE INDEX IF NOT EXISTS idx_email_notif_user ON email_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_push_notif_user ON push_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_spending_user ON spending_insights(user_id);
CREATE INDEX IF NOT EXISTS idx_beneficiaries_user ON beneficiaries(user_id);
CREATE INDEX IF NOT EXISTS idx_fraud_flags_user_reviewed ON fraud_flags(user_id, reviewed);
