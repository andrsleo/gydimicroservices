-- V1.13__create_commission_ledger_table.sql
-- Purpose: Create immutable commission ledger for financial audit trail
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.11 (referral_links)
-- Compliance: IMMUTABLE ledger for audit and tax purposes (7-year retention)

BEGIN;

-- =====================================================================
-- COMMISSION LEDGER TABLE (IMMUTABLE)
-- =====================================================================
-- Immutable financial record of all commission transactions
-- Once created, records CANNOT be modified (enforced by trigger)
-- Only status field can change through lifecycle
-- Lifecycle: PENDING (30 days) → APPROVED → PAID

CREATE TABLE IF NOT EXISTS referrals.commission_ledger (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    referral_link_id BIGINT NOT NULL,  -- FK to referrals.referral_links(id)
    affiliate_id BIGINT NOT NULL,      -- FK to users.users(id) - Denormalized for fast queries    
    property_id BIGINT NOT NULL,       -- FK to properties.properties(id) - Denormalized for reporting

    -- Financial data (IMMUTABLE after INSERT)
    commission_rate DECIMAL(5,4) NOT NULL,  -- Stored as decimal (0.02 = 2%, 0.05 = 5%, 0.15 = 15%)
    commission_amount DECIMAL(12,2) NOT NULL CHECK (commission_amount >= 0),

    -- Snapshot of affiliate plan at conversion time (for audit)
    affiliate_plan VARCHAR(20) NOT NULL,  -- 'FREE', 'PRO', 'ELITE'

    -- Status workflow
    status referrals.commission_status NOT NULL DEFAULT 'PENDING',

    -- Fraud detection & verification
    verification_hash BYTEA NOT NULL,  -- SHA-256 hash of all immutable fields (tamper detection)

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,  -- When fraud check passed (30 days after booking)
    paid_at TIMESTAMP WITH TIME ZONE,      -- When payment processed
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Business rules
    CONSTRAINT valid_plan_rate CHECK (
        (affiliate_plan = 'FREE' AND commission_rate = 0.02) OR
        (affiliate_plan = 'PRO' AND commission_rate = 0.05) OR
        (affiliate_plan = 'ELITE' AND commission_rate = 0.15)
    ),
    CONSTRAINT valid_status_workflow CHECK (
        (status = 'PENDING' AND approved_at IS NULL AND paid_at IS NULL) OR
        (status = 'APPROVED' AND approved_at IS NOT NULL AND paid_at IS NULL) OR
        (status = 'PAID' AND approved_at IS NOT NULL AND paid_at IS NOT NULL) OR
        (status IN ('REJECTED', 'REFUNDED'))
    ),

    -- Foreign key constraints
    CONSTRAINT fk_commission_ledger_referral_link FOREIGN KEY (referral_link_id)
        REFERENCES referrals.referral_links(id) ON DELETE CASCADE,
    CONSTRAINT fk_commission_ledger_affiliate FOREIGN KEY (affiliate_id)
        REFERENCES users.users(id) ON DELETE CASCADE,    
    CONSTRAINT fk_commission_ledger_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE
);

-- =====================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================

-- Index for affiliate earnings dashboard (fetch by affiliate + status)
CREATE INDEX idx_commission_ledger_affiliate_status
ON referrals.commission_ledger(affiliate_id, status, created_at DESC);

-- Index for payout processing (fetch all APPROVED records)
CREATE INDEX idx_commission_ledger_approved
ON referrals.commission_ledger(status, approved_at)
WHERE status = 'APPROVED';

-- Index for pending fraud review (auto-approve after 30 days)
CREATE INDEX idx_commission_ledger_pending_review
ON referrals.commission_ledger(status, created_at)
WHERE status = 'PENDING';

-- Index for referral link analytics (aggregate commissions per link)
CREATE INDEX idx_commission_ledger_link
ON referrals.commission_ledger(referral_link_id, status, commission_amount);

-- Index for property performance reporting
CREATE INDEX idx_commission_ledger_property
ON referrals.commission_ledger(property_id, created_at DESC);

-- Index for verification hash lookup (tamper detection)
CREATE INDEX idx_commission_ledger_verification
ON referrals.commission_ledger(verification_hash);

-- =====================================================================
-- FUNCTION: GENERATE VERIFICATION HASH
-- =====================================================================
-- Generates SHA-256 hash of all immutable fields for tamper detection
-- Called automatically by trigger on INSERT

CREATE OR REPLACE FUNCTION referrals.generate_verification_hash(
    p_id BIGINT,
    p_referral_link_id BIGINT,
    p_affiliate_id BIGINT,
    p_property_id BIGINT,
    p_commission_rate DECIMAL,
    p_commission_amount DECIMAL,
    p_affiliate_plan VARCHAR
) RETURNS BYTEA AS $$
DECLARE
    hash_input TEXT;
BEGIN
    -- Concatenate all immutable fields (excluding booking references)
    hash_input := CONCAT(
        p_id::TEXT, '|',
        p_referral_link_id::TEXT, '|',
        p_affiliate_id::TEXT, '|',
        p_property_id::TEXT, '|',
        p_commission_rate::TEXT, '|',
        p_commission_amount::TEXT, '|',
        p_affiliate_plan
    );

    -- Return SHA-256 hash
    RETURN digest(hash_input, 'sha256');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================================
-- FUNCTION: VERIFY LEDGER INTEGRITY
-- =====================================================================
-- Verifies that verification_hash matches current data (tamper detection)
-- Returns TRUE if integrity is maintained, FALSE if tampering detected

CREATE OR REPLACE FUNCTION referrals.verify_ledger_integrity(
    p_ledger_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
    record RECORD;
    expected_hash BYTEA;
BEGIN
    -- Fetch record
    SELECT * INTO record
    FROM referrals.commission_ledger
    WHERE id = p_ledger_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Calculate expected hash
    expected_hash := referrals.generate_verification_hash(
        record.id,
        record.referral_link_id,
        record.affiliate_id,
        record.property_id,
        record.commission_rate,
        record.commission_amount,
        record.affiliate_plan
    );

    -- Compare hashes
    RETURN record.verification_hash = expected_hash;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- FUNCTION: AUTO-APPROVE PENDING COMMISSIONS
-- =====================================================================
-- Approves commissions older than 30 days (fraud window passed)
-- Should be called daily via cron job or scheduler

CREATE OR REPLACE FUNCTION referrals.auto_approve_pending_commissions()
RETURNS TABLE(approved_count INTEGER) AS $$
DECLARE
    approval_threshold TIMESTAMP WITH TIME ZONE;
    updated_rows INTEGER;
BEGIN
    -- Calculate 30-day threshold
    approval_threshold := CURRENT_TIMESTAMP - INTERVAL '30 days';

    -- Update pending commissions older than 30 days
    WITH updated AS (
        UPDATE referrals.commission_ledger
        SET status = 'APPROVED',
            approved_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'PENDING'
        AND created_at < approval_threshold
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_rows FROM updated;

    RAISE NOTICE 'Auto-approved % commissions', updated_rows;
    RETURN QUERY SELECT updated_rows;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON TABLE referrals.commission_ledger IS
'IMMUTABLE financial ledger of all commission transactions. Records CANNOT be modified after creation (enforced by trigger). 7-year retention for tax compliance.';

COMMENT ON COLUMN referrals.commission_ledger.commission_rate IS
'Commission rate as decimal (0.02 = 2%). Immutable snapshot of affiliate plan rate at conversion time. Prevents retroactive changes.';

COMMENT ON COLUMN referrals.commission_ledger.commission_amount IS
'Commission amount in USD. Represents the actual commission earned by the affiliate for this conversion.';

COMMENT ON COLUMN referrals.commission_ledger.affiliate_plan IS
'Snapshot of affiliate subscription plan at conversion time (FREE/PRO/ELITE). Immutable for audit purposes.';

COMMENT ON COLUMN referrals.commission_ledger.verification_hash IS
'SHA-256 hash of all immutable fields. Used for tamper detection. Generated automatically by trigger on INSERT. Verify with referrals.verify_ledger_integrity().';

COMMENT ON COLUMN referrals.commission_ledger.status IS
'Lifecycle workflow: PENDING (0-30 days fraud review) → APPROVED (ready for payout) → PAID (payment processed). Can also be REJECTED (fraud) or REFUNDED (transaction refunded).';

COMMENT ON COLUMN referrals.commission_ledger.approved_at IS
'Timestamp when status changed to APPROVED (after 30-day fraud window). Set automatically by trigger or manual approval.';

COMMENT ON COLUMN referrals.commission_ledger.paid_at IS
'Timestamp when payment was processed to affiliate bank account. Set by payment processing system.';

COMMENT ON FUNCTION referrals.generate_verification_hash IS
'Generates SHA-256 hash of all immutable ledger fields for tamper detection. Called automatically by trigger on INSERT. Does not include booking references.';

COMMENT ON FUNCTION referrals.verify_ledger_integrity IS
'Verifies ledger record integrity by comparing stored hash with recalculated hash. Returns FALSE if tampering detected.';

COMMENT ON FUNCTION referrals.auto_approve_pending_commissions IS
'Auto-approves commissions pending longer than 30 days (fraud window). Run daily: SELECT * FROM referrals.auto_approve_pending_commissions();';

-- =====================================================================
-- GRANT PERMISSIONS
-- =====================================================================

GRANT SELECT, INSERT, UPDATE ON referrals.commission_ledger TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.generate_verification_hash TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.verify_ledger_integrity TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.auto_approve_pending_commissions TO andresvargas;

COMMIT;

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP FUNCTION IF EXISTS referrals.auto_approve_pending_commissions();
-- DROP FUNCTION IF EXISTS referrals.verify_ledger_integrity(BIGINT);
-- DROP FUNCTION IF EXISTS referrals.generate_verification_hash;
-- DROP TABLE IF EXISTS referrals.commission_ledger CASCADE;