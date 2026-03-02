-- V6: Add missing entities from UPI spec sections 1-2.8
-- TPAP model, bill payments (BBPS), settlement guarantee fund,
-- PSP sponsor bank, category-based limits, intent payments

-- ── 1. PSP sponsor bank relationship ──
ALTER TABLE psp ADD COLUMN IF NOT EXISTS sponsor_bank_code VARCHAR(20);

-- ── 2. TPAP (Third Party Application Provider) ──
CREATE TABLE IF NOT EXISTS tpap (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tpap_id         VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(200) NOT NULL,
    sponsor_psp_id  UUID NOT NULL REFERENCES psp(id),
    sponsor_bank_code VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    nrb_license_number VARCHAR(50),
    nrb_license_expiry DATE,
    technical_contact_email VARCHAR(200),
    technical_contact_phone VARCHAR(20),
    api_key_hash    VARCHAR(200),
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- ── 3. Bill Payment (BBPS equivalent) — Biller Registry ──
CREATE TABLE IF NOT EXISTS biller (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    biller_id       VARCHAR(50) UNIQUE NOT NULL,
    biller_name     VARCHAR(200) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    sub_category    VARCHAR(50),
    bank_code       VARCHAR(20) NOT NULL,
    settlement_account VARCHAR(50) NOT NULL,
    is_adhoc        BOOLEAN DEFAULT false,
    is_active       BOOLEAN DEFAULT true,
    fetch_supported BOOLEAN DEFAULT false,
    payment_modes   VARCHAR(200) DEFAULT 'ONLINE',
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bill (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    biller_id       UUID NOT NULL REFERENCES biller(id),
    customer_identifier VARCHAR(100) NOT NULL,
    bill_number     VARCHAR(100),
    bill_date       DATE,
    due_date        DATE,
    amount_paisa    BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payer_vpa       VARCHAR(100),
    transaction_id  UUID,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- ── 4. Settlement Guarantee Fund ──
CREATE TABLE IF NOT EXISTS settlement_guarantee_fund (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_code       VARCHAR(20) NOT NULL,
    contribution_paisa BIGINT NOT NULL,
    fund_date       DATE NOT NULL,
    total_fund_paisa BIGINT NOT NULL,
    utilization_paisa BIGINT DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    nrb_approved    BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- ── 5. Category-based transaction limits ──
CREATE TABLE IF NOT EXISTS category_transaction_limit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category        VARCHAR(50) NOT NULL,
    per_txn_limit_paisa BIGINT NOT NULL,
    daily_limit_paisa BIGINT NOT NULL,
    description     VARCHAR(200),
    is_active       BOOLEAN DEFAULT true,
    effective_from  DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE(category, effective_from)
);

-- Insert default NRB category limits
INSERT INTO category_transaction_limit (id, category, per_txn_limit_paisa, daily_limit_paisa, description, effective_from) VALUES
    (gen_random_uuid(), 'P2P', 10000000, 20000000, 'Person to Person default limit Rs 1L/txn, Rs 2L/day', CURRENT_DATE),
    (gen_random_uuid(), 'P2M', 10000000, 20000000, 'Person to Merchant default limit Rs 1L/txn, Rs 2L/day', CURRENT_DATE),
    (gen_random_uuid(), 'BILL_PAYMENT', 20000000, 50000000, 'Bill payments enhanced limit Rs 2L/txn, Rs 5L/day', CURRENT_DATE),
    (gen_random_uuid(), 'INSURANCE', 20000000, 50000000, 'Insurance premiums enhanced limit Rs 2L/txn, Rs 5L/day', CURRENT_DATE),
    (gen_random_uuid(), 'EDUCATION', 50000000, 50000000, 'Education fees enhanced limit Rs 5L/txn, Rs 5L/day', CURRENT_DATE),
    (gen_random_uuid(), 'TAX', 50000000, 50000000, 'Tax payments enhanced limit Rs 5L/txn, Rs 5L/day', CURRENT_DATE),
    (gen_random_uuid(), 'HOSPITAL', 50000000, 50000000, 'Hospital/medical enhanced limit Rs 5L/txn, Rs 5L/day', CURRENT_DATE)
ON CONFLICT DO NOTHING;

-- ── 6. Intent Payment Log ──
CREATE TABLE IF NOT EXISTS intent_payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    intent_ref      VARCHAR(50) UNIQUE NOT NULL,
    merchant_vpa    VARCHAR(100) NOT NULL,
    amount_paisa    BIGINT,
    note            VARCHAR(500),
    merchant_name   VARCHAR(200),
    intent_url      VARCHAR(2000) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    transaction_id  UUID,
    payer_vpa       VARCHAR(100),
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);
