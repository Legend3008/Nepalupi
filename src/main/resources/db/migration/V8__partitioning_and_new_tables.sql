-- =============================================================
-- V8: Database Partitioning Strategy & Performance Optimizations
-- Section 17.1: Table partitioning for high-volume tables
-- =============================================================

-- ─────────────────────────────────────────────
-- 1. Transaction Archive Partitioning (by month)
-- Range partitioning on initiated_at for efficient historical queries
-- ─────────────────────────────────────────────

-- Create partitioned archive table (if transactions exceed 10M rows, 
-- migrate to this structure)
CREATE TABLE IF NOT EXISTS transactions_partitioned (
    LIKE transactions INCLUDING ALL
) PARTITION BY RANGE (initiated_at);

-- Create monthly partitions (12 months ahead)
CREATE TABLE IF NOT EXISTS transactions_y2025_m01 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m02 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m03 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m04 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m05 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m06 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m07 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m08 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m09 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m10 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m11 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE IF NOT EXISTS transactions_y2025_m12 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 2026 Q1 partitions (ahead-of-time)
CREATE TABLE IF NOT EXISTS transactions_y2026_m01 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE IF NOT EXISTS transactions_y2026_m02 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE IF NOT EXISTS transactions_y2026_m03 PARTITION OF transactions_partitioned
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- ─────────────────────────────────────────────
-- 2. Audit Log Partitioning (by month)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transaction_audit_log_partitioned (
    LIKE transaction_audit_log INCLUDING ALL
) PARTITION BY RANGE (changed_at);

CREATE TABLE IF NOT EXISTS audit_log_y2025_m01 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE IF NOT EXISTS audit_log_y2025_m02 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE IF NOT EXISTS audit_log_y2025_m03 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS audit_log_y2025_m04 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE IF NOT EXISTS audit_log_y2025_m05 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE IF NOT EXISTS audit_log_y2025_m06 PARTITION OF transaction_audit_log_partitioned
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

-- ─────────────────────────────────────────────
-- 3. Monthly Transaction Stats table
-- For monthly limit tracking (S5.3)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS monthly_transaction_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    stats_month VARCHAR(7) NOT NULL,  -- format: '2025-01'
    total_amount_paisa BIGINT NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, stats_month)
);

CREATE INDEX IF NOT EXISTS idx_monthly_stats_user_month 
    ON monthly_transaction_stats(user_id, stats_month);

-- ─────────────────────────────────────────────
-- 4. IPO Payment table (S19.5)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ipo_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    ipo_name VARCHAR(200) NOT NULL,
    ipo_code VARCHAR(50) NOT NULL,
    company_name VARCHAR(300) NOT NULL,
    kitta_applied INT NOT NULL,
    amount_per_kitta_paisa BIGINT NOT NULL,
    total_amount_paisa BIGINT NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    demat_number VARCHAR(30) NOT NULL,
    boid VARCHAR(20) NOT NULL,
    transaction_id UUID REFERENCES transactions(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    application_number VARCHAR(50),
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    verified_at TIMESTAMP WITH TIME ZONE,
    allotment_kitta INT,
    refund_amount_paisa BIGINT DEFAULT 0,
    refund_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ipo_payments_user ON ipo_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_ipo_payments_ipo_code ON ipo_payments(ipo_code);
CREATE INDEX IF NOT EXISTS idx_ipo_payments_status ON ipo_payments(status);
CREATE INDEX IF NOT EXISTS idx_ipo_payments_boid ON ipo_payments(boid);

-- ─────────────────────────────────────────────
-- 5. Tax Payment table (S19.6)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tax_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    tax_type VARCHAR(50) NOT NULL,  -- INCOME_TAX, VAT, TDS, CUSTOM_DUTY, VEHICLE_TAX
    taxpayer_pan VARCHAR(20) NOT NULL,
    taxpayer_name VARCHAR(200) NOT NULL,
    fiscal_year VARCHAR(10) NOT NULL,  -- e.g., '2081/82'
    tax_period VARCHAR(20),
    amount_paisa BIGINT NOT NULL,
    fine_paisa BIGINT DEFAULT 0,
    interest_paisa BIGINT DEFAULT 0,
    total_amount_paisa BIGINT NOT NULL,
    ird_office_code VARCHAR(10),
    voucher_number VARCHAR(50),
    transaction_id UUID REFERENCES transactions(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_receipt_number VARCHAR(50),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tax_payments_user ON tax_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_tax_payments_pan ON tax_payments(taxpayer_pan);
CREATE INDEX IF NOT EXISTS idx_tax_payments_type ON tax_payments(tax_type);
CREATE INDEX IF NOT EXISTS idx_tax_payments_fiscal_year ON tax_payments(fiscal_year);

-- ─────────────────────────────────────────────
-- 6. Biometric Enrollment table (S12.3)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS biometric_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    device_id VARCHAR(200) NOT NULL,
    biometric_type VARCHAR(20) NOT NULL,  -- FINGERPRINT, FACE
    public_key_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failure_count INT DEFAULT 0,
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, device_id, biometric_type)
);

CREATE INDEX IF NOT EXISTS idx_biometric_user ON biometric_enrollments(user_id);

-- ─────────────────────────────────────────────
-- 7. Session Tracking table (S12.5)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS psp_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL UNIQUE,
    psp_code VARCHAR(50) NOT NULL,
    source_ip VARCHAR(50),
    device_info VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_psp_sessions_psp ON psp_sessions(psp_code, status);
CREATE INDEX IF NOT EXISTS idx_psp_sessions_expires ON psp_sessions(expires_at);

-- ─────────────────────────────────────────────
-- 8. SLO Tracking table (S18.6)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS slo_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slo_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,  -- AVAILABILITY, LATENCY, ERROR_RATE, THROUGHPUT
    target_value DECIMAL(10,4) NOT NULL,
    actual_value DECIMAL(10,4) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    is_met BOOLEAN NOT NULL,
    error_budget_remaining DECIMAL(10,4),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slo_metrics_name ON slo_metrics(slo_name, period_start);
CREATE INDEX IF NOT EXISTS idx_slo_metrics_met ON slo_metrics(is_met, period_start);

-- ─────────────────────────────────────────────
-- 9. Performance indexes for partitioned queries
-- ─────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_transactions_initiated_month 
    ON transactions(initiated_at);

CREATE INDEX IF NOT EXISTS idx_transactions_status_date 
    ON transactions(status, initiated_at);

CREATE INDEX IF NOT EXISTS idx_transactions_payer_date 
    ON transactions(payer_vpa, initiated_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_payee_date 
    ON transactions(payee_vpa, initiated_at DESC);
