-- =====================================================
-- V4: Modules 13-16 — Security, Bank Cert, PSP Cert, Launch
-- =====================================================

-- =====================================================
-- MODULE 13: Security & Penetration Testing Framework
-- =====================================================

-- Security audit records
CREATE TABLE security_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_type VARCHAR(50) NOT NULL,           -- INTERNAL, EXTERNAL_PENTEST, RED_TEAM, VULNERABILITY_SCAN
    audit_status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    auditor_name VARCHAR(200),
    auditor_firm VARCHAR(200),
    scope_description TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    next_audit_due TIMESTAMPTZ,
    total_findings INT DEFAULT 0,
    critical_findings INT DEFAULT 0,
    high_findings INT DEFAULT 0,
    medium_findings INT DEFAULT 0,
    low_findings INT DEFAULT 0,
    report_url VARCHAR(500),
    executive_summary TEXT,
    nrb_submitted BOOLEAN DEFAULT FALSE,
    nrb_submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Individual security findings/vulnerabilities
CREATE TABLE security_finding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id UUID NOT NULL REFERENCES security_audit(id),
    finding_reference VARCHAR(50) NOT NULL,     -- e.g. VULN-2026-001
    title VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,              -- CRITICAL, HIGH, MEDIUM, LOW, INFO
    category VARCHAR(50) NOT NULL,              -- SQL_INJECTION, XSS, CRYPTO, AUTH_BYPASS, etc.
    affected_component VARCHAR(200),
    reproduction_steps TEXT,
    evidence TEXT,
    recommended_fix TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, FIXED, VERIFIED, ACCEPTED_RISK, FALSE_POSITIVE
    assigned_to VARCHAR(100),
    remediation_deadline TIMESTAMPTZ,
    fixed_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    verified_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_finding_audit ON security_finding(audit_id);
CREATE INDEX idx_security_finding_status ON security_finding(status);
CREATE INDEX idx_security_finding_severity ON security_finding(severity);

-- Vulnerability disclosure / bug bounty
CREATE TABLE vulnerability_disclosure (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_reference VARCHAR(100),
    reporter_name VARCHAR(200),
    reporter_email VARCHAR(200),
    reporter_alias VARCHAR(100),           -- handle/alias for public credit
    title VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    affected_system VARCHAR(200),
    reproduction_steps TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',  -- RECEIVED, TRIAGED, CONFIRMED, IN_PROGRESS, FIXED, DUPLICATE, OUT_OF_SCOPE, INVALID
    bounty_amount_paisa BIGINT DEFAULT 0,
    bounty_paid BOOLEAN DEFAULT FALSE,
    bounty_paid_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    triaged_at TIMESTAMPTZ,
    fixed_at TIMESTAMPTZ,
    disclosed_publicly BOOLEAN DEFAULT FALSE,
    public_disclosure_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vuln_disclosure_status ON vulnerability_disclosure(status);

-- Certificate inventory for rotation tracking
CREATE TABLE certificate_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cert_name VARCHAR(200) NOT NULL,
    cert_type VARCHAR(50) NOT NULL,          -- TLS_SERVER, TLS_CLIENT, MTLS_SERVICE, CODE_SIGNING, HSM_KEY
    subject_cn VARCHAR(300),
    issuer VARCHAR(300),
    serial_number VARCHAR(200),
    fingerprint_sha256 VARCHAR(100),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    auto_rotate BOOLEAN DEFAULT FALSE,
    rotation_period_days INT,
    last_rotated_at TIMESTAMPTZ,
    next_rotation_due TIMESTAMPTZ,
    associated_service VARCHAR(100),        -- which service uses this cert
    key_store_location VARCHAR(300),        -- HSM slot, vault path, etc.
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRING_SOON, EXPIRED, REVOKED, ROTATED
    alert_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cert_status ON certificate_inventory(status);
CREATE INDEX idx_cert_expiry ON certificate_inventory(valid_until);

-- Security incident (distinct from operations incident — focused on breach/attack)
CREATE TABLE security_incident (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_reference VARCHAR(50) NOT NULL UNIQUE,  -- SEC-INC-2026-001
    incident_type VARCHAR(50) NOT NULL,               -- DATA_BREACH, UNAUTHORIZED_ACCESS, DDOS, MALWARE, INSIDER_THREAT, PHISHING
    severity VARCHAR(20) NOT NULL,                    -- CRITICAL, HIGH, MEDIUM, LOW
    title VARCHAR(300) NOT NULL,
    description TEXT,
    attack_vector VARCHAR(100),
    affected_systems TEXT,
    data_compromised BOOLEAN DEFAULT FALSE,
    data_compromised_description TEXT,
    users_affected_count INT DEFAULT 0,
    financial_impact_paisa BIGINT DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DETECTED',   -- DETECTED, CONTAINED, INVESTIGATING, ERADICATED, RECOVERED, CLOSED
    detected_at TIMESTAMPTZ NOT NULL,
    contained_at TIMESTAMPTZ,
    eradicated_at TIMESTAMPTZ,
    recovered_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    nrb_notified BOOLEAN DEFAULT FALSE,
    nrb_notified_at TIMESTAMPTZ,
    users_notified BOOLEAN DEFAULT FALSE,
    users_notified_at TIMESTAMPTZ,
    law_enforcement_notified BOOLEAN DEFAULT FALSE,
    incident_commander VARCHAR(100),
    root_cause TEXT,
    lessons_learned TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_incident_status ON security_incident(status);
CREATE INDEX idx_security_incident_type ON security_incident(incident_type);

-- WAF rule configurations
CREATE TABLE waf_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,          -- SQL_INJECTION, XSS, REQUEST_SMUGGLING, RATE_LIMIT, GEO_BLOCK, CUSTOM
    action VARCHAR(20) NOT NULL DEFAULT 'BLOCK',  -- BLOCK, LOG, CHALLENGE
    pattern TEXT,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 100,
    hits_total BIGINT DEFAULT 0,
    last_hit_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- MODULE 14: Bank Integration Certification
-- =====================================================

CREATE TABLE bank_certification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_code VARCHAR(20) NOT NULL,
    bank_name VARCHAR(200) NOT NULL,
    stage VARCHAR(50) NOT NULL DEFAULT 'TECHNICAL_ONBOARDING',  -- TECHNICAL_ONBOARDING, DOCUMENTATION, SELF_CERTIFICATION, FORMAL_CERTIFICATION, PARALLEL_RUNNING, FULL_PRODUCTION
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, PASSED, FAILED, SUSPENDED
    technical_contact_name VARCHAR(200),
    technical_contact_email VARCHAR(200),
    technical_contact_phone VARCHAR(20),
    agreement_signed BOOLEAN DEFAULT FALSE,
    agreement_signed_at TIMESTAMPTZ,
    sandbox_credentials_issued BOOLEAN DEFAULT FALSE,
    sandbox_credentials_issued_at TIMESTAMPTZ,
    documentation_delivered BOOLEAN DEFAULT FALSE,
    documentation_delivered_at TIMESTAMPTZ,
    self_cert_submitted BOOLEAN DEFAULT FALSE,
    self_cert_submitted_at TIMESTAMPTZ,
    self_cert_passed BOOLEAN DEFAULT FALSE,
    formal_cert_scheduled_at TIMESTAMPTZ,
    formal_cert_completed_at TIMESTAMPTZ,
    formal_cert_passed BOOLEAN DEFAULT FALSE,
    mandatory_pass_rate NUMERIC(5,2),        -- percentage
    advisory_pass_rate NUMERIC(5,2),
    parallel_start_date DATE,
    parallel_end_date DATE,
    parallel_daily_limit INT DEFAULT 100,
    parallel_anomalies_found INT DEFAULT 0,
    production_go_live_date DATE,
    recertification_due_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bank_cert_code ON bank_certification(bank_code);
CREATE INDEX idx_bank_cert_stage ON bank_certification(stage);

-- Individual certification test cases
CREATE TABLE cert_test_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_code VARCHAR(50) NOT NULL UNIQUE,     -- e.g. TC-HP-001
    category VARCHAR(30) NOT NULL,             -- HAPPY_PATH, FAILURE, TIMEOUT, REVERSAL, SETTLEMENT
    is_mandatory BOOLEAN DEFAULT TRUE,
    title VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    expected_behavior TEXT NOT NULL,
    iso_message_template TEXT,                 -- template ISO 8583 message
    expected_response_code VARCHAR(10),
    timeout_seconds INT DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cert_test_category ON cert_test_case(category);

-- Test execution results per bank
CREATE TABLE cert_test_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certification_id UUID NOT NULL REFERENCES bank_certification(id),
    test_case_id UUID NOT NULL REFERENCES cert_test_case(id),
    execution_phase VARCHAR(30) NOT NULL,       -- SELF_CERTIFICATION, FORMAL_CERTIFICATION
    result VARCHAR(20) NOT NULL,                -- PASSED, FAILED, SKIPPED, ERROR
    request_sent TEXT,
    response_received TEXT,
    response_time_ms BIGINT,
    notes TEXT,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    executed_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cert_result_cert ON cert_test_result(certification_id);
CREATE INDEX idx_cert_result_test ON cert_test_result(test_case_id);

-- Bank performance monitoring (post-certification)
CREATE TABLE bank_performance_metric (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_code VARCHAR(20) NOT NULL,
    metric_date DATE NOT NULL,
    total_transactions BIGINT DEFAULT 0,
    successful_transactions BIGINT DEFAULT 0,
    failed_transactions BIGINT DEFAULT 0,
    timeout_count BIGINT DEFAULT 0,
    avg_response_time_ms BIGINT DEFAULT 0,
    p95_response_time_ms BIGINT DEFAULT 0,
    p99_response_time_ms BIGINT DEFAULT 0,
    settlement_accuracy_pct NUMERIC(5,2) DEFAULT 100.00,
    error_rate_pct NUMERIC(5,2) DEFAULT 0.00,
    performance_notice_sent BOOLEAN DEFAULT FALSE,
    performance_notice_sent_at TIMESTAMPTZ,
    below_network_average BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(bank_code, metric_date)
);

CREATE INDEX idx_bank_perf_code ON bank_performance_metric(bank_code);
CREATE INDEX idx_bank_perf_date ON bank_performance_metric(metric_date);

-- =====================================================
-- MODULE 15: PSP App Certification
-- =====================================================

CREATE TABLE psp_app_certification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id VARCHAR(50) NOT NULL,
    app_name VARCHAR(200) NOT NULL,
    app_platform VARCHAR(20) NOT NULL,          -- ANDROID, IOS, WEB
    app_version VARCHAR(30),
    sdk_version VARCHAR(30),
    stage VARCHAR(50) NOT NULL DEFAULT 'DESIGN_REVIEW',  -- DESIGN_REVIEW, SDK_INTEGRATION, FUNCTIONAL_TESTING, SECURITY_TESTING, PERFORMANCE_TESTING, COMPLIANCE_REVIEW, PILOT, LAUNCHED
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, PASSED, FAILED, CONDITIONALLY_PASSED, SUSPENDED
    -- Design review
    design_submitted BOOLEAN DEFAULT FALSE,
    design_submitted_at TIMESTAMPTZ,
    design_approved BOOLEAN DEFAULT FALSE,
    design_feedback TEXT,
    -- SDK integration
    sdk_verified BOOLEAN DEFAULT FALSE,
    cert_pinning_verified BOOLEAN DEFAULT FALSE,
    pin_pad_sdk_verified BOOLEAN DEFAULT FALSE,
    no_hardcoded_keys BOOLEAN DEFAULT FALSE,
    permissions_minimal BOOLEAN DEFAULT FALSE,
    screenshot_prevention_verified BOOLEAN DEFAULT FALSE,
    -- Functional testing
    functional_test_passed BOOLEAN DEFAULT FALSE,
    functional_test_notes TEXT,
    -- Security testing
    security_test_passed BOOLEAN DEFAULT FALSE,
    security_findings_count INT DEFAULT 0,
    security_critical_findings INT DEFAULT 0,
    security_test_report_url VARCHAR(500),
    -- Performance testing
    performance_test_passed BOOLEAN DEFAULT FALSE,
    avg_pin_screen_time_ms INT,
    avg_confirmation_time_ms INT,
    avg_history_load_time_ms INT,
    -- Compliance review
    compliance_passed BOOLEAN DEFAULT FALSE,
    terms_present BOOLEAN DEFAULT FALSE,
    privacy_policy_present BOOLEAN DEFAULT FALSE,
    grievance_officer_displayed BOOLEAN DEFAULT FALSE,
    dispute_accessible BOOLEAN DEFAULT FALSE,
    -- Pilot
    pilot_start_date DATE,
    pilot_end_date DATE,
    pilot_user_count INT DEFAULT 0,
    pilot_feedback_summary TEXT,
    -- Launch
    launched_at TIMESTAMPTZ,
    reviewer VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_psp_app_cert_psp ON psp_app_certification(psp_id);
CREATE INDEX idx_psp_app_cert_stage ON psp_app_certification(stage);

-- PSP app review checklist items
CREATE TABLE psp_app_review_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certification_id UUID NOT NULL REFERENCES psp_app_certification(id),
    review_stage VARCHAR(50) NOT NULL,          -- matches stage enum
    checklist_item VARCHAR(300) NOT NULL,
    is_mandatory BOOLEAN DEFAULT TRUE,
    result VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PASSED, FAILED, NOT_APPLICABLE
    reviewer_notes TEXT,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_psp_review_cert ON psp_app_review_item(certification_id);

-- PSP SDK version tracking
CREATE TABLE psp_sdk_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id VARCHAR(50) NOT NULL,
    app_platform VARCHAR(20) NOT NULL,
    current_sdk_version VARCHAR(30) NOT NULL,
    latest_available_version VARCHAR(30) NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    upgrade_required BOOLEAN DEFAULT FALSE,
    upgrade_deadline TIMESTAMPTZ,
    upgrade_notice_sent BOOLEAN DEFAULT FALSE,
    upgrade_notice_sent_at TIMESTAMPTZ,
    transactions_restricted BOOLEAN DEFAULT FALSE,
    restricted_at TIMESTAMPTZ,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_psp_sdk_psp ON psp_sdk_version(psp_id);

-- =====================================================
-- MODULE 16: Go-to-Market & Launch Sequencing
-- =====================================================

-- Launch phases and milestones
CREATE TABLE launch_phase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_number INT NOT NULL,
    phase_name VARCHAR(100) NOT NULL,           -- FOUNDATION, CONTROLLED_LAUNCH, ACCELERATED_GROWTH, ECOSYSTEM_EXPANSION
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, COMPLETED
    description TEXT,
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    registration_daily_cap INT,
    per_txn_limit_paisa BIGINT,
    daily_limit_paisa BIGINT,
    target_banks INT,
    target_psp_apps INT,
    target_merchants INT,
    target_registered_users INT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Launch readiness checklist
CREATE TABLE launch_checklist_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id UUID NOT NULL REFERENCES launch_phase(id),
    category VARCHAR(50) NOT NULL,             -- TECHNICAL, REGULATORY, BANKING, COMMERCIAL, OPERATIONAL
    title VARCHAR(300) NOT NULL,
    description TEXT,
    is_blocking BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED, BLOCKED
    owner VARCHAR(100),
    due_date DATE,
    completed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_launch_checklist_phase ON launch_checklist_item(phase_id);

-- Growth / KPI metrics tracking
CREATE TABLE launch_metric (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    -- User metrics
    total_registered_users BIGINT DEFAULT 0,
    new_registrations_today BIGINT DEFAULT 0,
    active_users_30d BIGINT DEFAULT 0,
    -- Transaction metrics
    total_transactions_today BIGINT DEFAULT 0,
    total_volume_paisa_today BIGINT DEFAULT 0,
    txn_success_rate_pct NUMERIC(5,2) DEFAULT 0.00,
    avg_txn_amount_paisa BIGINT DEFAULT 0,
    p2p_count BIGINT DEFAULT 0,
    p2m_count BIGINT DEFAULT 0,
    -- Merchant metrics
    total_active_merchants BIGINT DEFAULT 0,
    new_merchants_today BIGINT DEFAULT 0,
    -- Bank coverage
    total_banks_live INT DEFAULT 0,
    banking_coverage_pct NUMERIC(5,2) DEFAULT 0.00,  -- % of banked accounts reachable
    -- Settlement
    settlement_accuracy_pct NUMERIC(5,2) DEFAULT 100.00,
    reconciliation_breaks INT DEFAULT 0,
    -- PSP
    total_psp_apps_live INT DEFAULT 0,
    -- Regional
    kathmandu_merchants INT DEFAULT 0,
    pokhara_merchants INT DEFAULT 0,
    biratnagar_merchants INT DEFAULT 0,
    other_merchants INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(metric_date)
);

CREATE INDEX idx_launch_metric_date ON launch_metric(metric_date);

-- Merchant acquisition tracking
CREATE TABLE merchant_acquisition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID REFERENCES merchant(id),
    merchant_name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    footfall_category VARCHAR(20),              -- HIGH, MEDIUM, LOW
    acquisition_channel VARCHAR(50),            -- FIELD_SALES, ONLINE, PARTNER_REFERRAL, BANK_REFERRAL
    acquired_by VARCHAR(100),
    qr_deployed BOOLEAN DEFAULT FALSE,
    qr_deployed_at TIMESTAMPTZ,
    first_transaction_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT FALSE,
    monthly_txn_count INT DEFAULT 0,
    monthly_volume_paisa BIGINT DEFAULT 0,
    onboarded_at TIMESTAMPTZ,
    churned_at TIMESTAMPTZ,
    churn_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_acq_city ON merchant_acquisition(city);
CREATE INDEX idx_merchant_acq_active ON merchant_acquisition(is_active);

-- Government payment integration tracking
CREATE TABLE govt_payment_integration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_name VARCHAR(200) NOT NULL,
    payment_type VARCHAR(100) NOT NULL,         -- ELECTRICITY, WATER, PROPERTY_TAX, PASSPORT, OTHER
    integration_status VARCHAR(30) NOT NULL DEFAULT 'IDENTIFIED',  -- IDENTIFIED, IN_DISCUSSION, AGREEMENT_SIGNED, DEVELOPMENT, TESTING, LIVE
    contact_person VARCHAR(200),
    contact_email VARCHAR(200),
    estimated_monthly_volume BIGINT DEFAULT 0,
    estimated_monthly_amount_paisa BIGINT DEFAULT 0,
    agreement_signed_at TIMESTAMPTZ,
    go_live_date DATE,
    actual_monthly_volume BIGINT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Incentive program tracking
CREATE TABLE incentive_program (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    psp_id VARCHAR(50),
    program_name VARCHAR(200) NOT NULL,
    program_type VARCHAR(50) NOT NULL,          -- CASHBACK, REWARD_POINTS, REFERRAL, MERCHANT_DISCOUNT
    start_date DATE NOT NULL,
    end_date DATE,
    budget_paisa BIGINT DEFAULT 0,
    spent_paisa BIGINT DEFAULT 0,
    max_per_user_paisa BIGINT DEFAULT 0,
    max_per_txn_paisa BIGINT DEFAULT 0,
    eligible_txn_types VARCHAR(100),            -- P2P, P2M, BILL_PAY
    min_txn_amount_paisa BIGINT DEFAULT 0,
    total_redemptions BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incentive_psp ON incentive_program(psp_id);
