-- V36__remove_bookings_references.sql
-- Purpose: Remove all references to bookings from referral system
-- Author: CTO AI
-- Date: 2025-11-18
-- Rationale: Bookings bounded context eliminated - referral system no longer depends on bookings

BEGIN;

-- =====================================================================
-- STEP 1: DROP VIEWS THAT USE BOOKING FIELDS
-- =====================================================================

DROP VIEW IF EXISTS referrals.v_property_referral_stats;
DROP VIEW IF EXISTS referrals.v_affiliate_earnings_history;

-- =====================================================================
-- STEP 2: DROP TRIGGERS THAT REFERENCE BOOKINGS
-- =====================================================================

DROP TRIGGER IF EXISTS tr_prevent_self_referral ON referrals.commission_ledger;
DROP TRIGGER IF EXISTS tr_audit_commission_status_change ON referrals.commission_ledger;
DROP TRIGGER IF EXISTS tr_enforce_ledger_immutability ON referrals.commission_ledger;
DROP TRIGGER IF EXISTS tr_auto_generate_verification_hash ON referrals.commission_ledger;

-- =====================================================================
-- STEP 3: DROP FUNCTIONS THAT USE BOOKING PARAMETERS
-- =====================================================================

DROP FUNCTION IF EXISTS referrals.prevent_self_referral();
DROP FUNCTION IF EXISTS referrals.audit_commission_status_change();
DROP FUNCTION IF EXISTS referrals.enforce_ledger_immutability();
DROP FUNCTION IF EXISTS referrals.auto_generate_verification_hash();
DROP FUNCTION IF EXISTS referrals.generate_verification_hash(BIGINT, BIGINT, BIGINT, BIGINT, BIGINT, DECIMAL, DECIMAL, DECIMAL, VARCHAR);
DROP FUNCTION IF EXISTS referrals.verify_ledger_integrity(BIGINT);

-- =====================================================================
-- STEP 4: ALTER COMMISSION_LEDGER TABLE - REMOVE BOOKING COLUMNS
-- =====================================================================

-- Drop constraints that reference booking columns
ALTER TABLE referrals.commission_ledger
DROP CONSTRAINT IF EXISTS valid_commission_calculation;

-- Drop the booking columns
ALTER TABLE referrals.commission_ledger
DROP COLUMN IF EXISTS booking_id;

ALTER TABLE referrals.commission_ledger
DROP COLUMN IF EXISTS booking_amount;

-- =====================================================================
-- STEP 5: RECREATE GENERATE_VERIFICATION_HASH (WITHOUT BOOKING_ID)
-- =====================================================================

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

COMMENT ON FUNCTION referrals.generate_verification_hash IS
'Generates SHA-256 hash of all immutable ledger fields for tamper detection. Called automatically by trigger on INSERT. Updated to remove booking references.';

-- =====================================================================
-- STEP 6: RECREATE VERIFY_LEDGER_INTEGRITY (WITHOUT BOOKING_ID)
-- =====================================================================

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

COMMENT ON FUNCTION referrals.verify_ledger_integrity IS
'Verifies ledger record integrity by comparing stored hash with recalculated hash. Returns FALSE if tampering detected. Updated to remove booking references.';

-- =====================================================================
-- STEP 7: RECREATE AUTO_GENERATE_VERIFICATION_HASH TRIGGER
-- =====================================================================

CREATE OR REPLACE FUNCTION referrals.auto_generate_verification_hash()
RETURNS TRIGGER AS $$
BEGIN
    -- Only generate if not already provided
    IF NEW.verification_hash IS NULL OR NEW.verification_hash = '' THEN
        NEW.verification_hash = referrals.generate_verification_hash(
            NEW.id,
            NEW.referral_link_id,
            NEW.affiliate_id,
            NEW.property_id,
            NEW.commission_rate,
            NEW.commission_amount,
            NEW.affiliate_plan
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_auto_generate_verification_hash
    BEFORE INSERT ON referrals.commission_ledger
    FOR EACH ROW
    EXECUTE FUNCTION referrals.auto_generate_verification_hash();

COMMENT ON FUNCTION referrals.auto_generate_verification_hash IS
'Auto-generates verification hash on commission INSERT for tamper detection. Updated to remove booking references.';

-- =====================================================================
-- STEP 8: RECREATE ENFORCE_LEDGER_IMMUTABILITY (WITHOUT BOOKING_ID)
-- =====================================================================

CREATE OR REPLACE FUNCTION referrals.enforce_ledger_immutability()
RETURNS TRIGGER AS $$
BEGIN
    -- Allow status changes (normal workflow)
    IF OLD.status != NEW.status THEN
        -- Status change is allowed
        -- But verify other fields remain unchanged
        IF OLD.referral_link_id != NEW.referral_link_id OR
           OLD.affiliate_id != NEW.affiliate_id OR
           OLD.property_id != NEW.property_id OR
           OLD.commission_rate != NEW.commission_rate OR
           OLD.commission_amount != NEW.commission_amount OR
           OLD.affiliate_plan != NEW.affiliate_plan OR
           OLD.verification_hash != NEW.verification_hash THEN
            RAISE EXCEPTION 'Immutable ledger violation: Cannot modify financial fields (only status can change)';
        END IF;

        -- Update workflow timestamps
        IF NEW.status = 'APPROVED' AND OLD.status = 'PENDING' THEN
            NEW.approved_at = CURRENT_TIMESTAMP;
        ELSIF NEW.status = 'PAID' AND OLD.status = 'APPROVED' THEN
            NEW.paid_at = CURRENT_TIMESTAMP;
        END IF;
    ELSE
        -- No status change = no changes allowed
        RAISE EXCEPTION 'Immutable ledger violation: Cannot modify commission records after creation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_enforce_ledger_immutability
    BEFORE UPDATE ON referrals.commission_ledger
    FOR EACH ROW
    EXECUTE FUNCTION referrals.enforce_ledger_immutability();

COMMENT ON FUNCTION referrals.enforce_ledger_immutability IS
'Enforces immutability of commission_ledger. Only status field can change. All other fields are locked after INSERT. Updated to remove booking_id checks.';

-- =====================================================================
-- STEP 9: RECREATE AUDIT_COMMISSION_STATUS_CHANGE (WITHOUT BOOKING_ID)
-- =====================================================================

CREATE OR REPLACE FUNCTION referrals.audit_commission_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO referrals.security_audit_log (
            event_type,
            user_id,
            details
        ) VALUES (
            'COMMISSION_STATUS_CHANGE',
            NEW.affiliate_id,
            jsonb_build_object(
                'commission_id', NEW.id,
                'old_status', OLD.status,
                'new_status', NEW.status,
                'commission_amount', NEW.commission_amount,
                'changed_at', CURRENT_TIMESTAMP,
                'changed_by', CURRENT_USER
            )
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_audit_commission_status_change
    AFTER UPDATE ON referrals.commission_ledger
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION referrals.audit_commission_status_change();

COMMENT ON FUNCTION referrals.audit_commission_status_change IS
'Logs all commission status changes to security_audit_log for compliance and debugging. Updated to remove booking_id.';

-- =====================================================================
-- STEP 10: RECREATE V_PROPERTY_REFERRAL_STATS VIEW (WITHOUT BOOKINGS)
-- =====================================================================

CREATE OR REPLACE VIEW referrals.v_property_referral_stats AS
SELECT
    p.id AS property_id,
    p.title AS property_title,
    p.listing_type,
    COUNT(DISTINCT rl.id) AS total_referral_links,
    COUNT(DISTINCT rl.affiliate_id) AS unique_affiliates,
    SUM(rl.clicks_count) AS total_clicks,
    SUM(rl.conversions_count) AS total_conversions,
    CASE
        WHEN SUM(rl.clicks_count) > 0 THEN
            ROUND((SUM(rl.conversions_count)::DECIMAL / SUM(rl.clicks_count)::DECIMAL * 100), 2)
        ELSE 0
    END AS conversion_rate_percent,
    SUM(cl.commission_amount) AS total_commissions_paid,
    ROUND(AVG(cl.commission_rate) * 100, 2) AS avg_commission_rate_percent
FROM properties.properties p
LEFT JOIN referrals.referral_links rl ON rl.property_id = p.id AND rl.deleted_at IS NULL
LEFT JOIN referrals.commission_ledger cl ON cl.property_id = p.id
GROUP BY p.id, p.title, p.listing_type
HAVING COUNT(DISTINCT rl.id) > 0  -- Only properties with referral links
ORDER BY SUM(cl.commission_amount) DESC NULLS LAST;

COMMENT ON VIEW referrals.v_property_referral_stats IS
'Real-time property performance through referral program. Shows which properties generate most commissions. Updated to remove booking_amount references.';

-- =====================================================================
-- STEP 11: RECREATE V_AFFILIATE_EARNINGS_HISTORY VIEW (WITHOUT BOOKINGS)
-- =====================================================================

CREATE OR REPLACE VIEW referrals.v_affiliate_earnings_history AS
SELECT
    cl.id AS commission_id,
    cl.affiliate_id,
    cl.property_id,
    p.title AS property_title,
    cl.commission_rate,
    cl.commission_amount,
    cl.affiliate_plan,
    cl.status,
    cl.created_at,
    cl.approved_at,
    cl.paid_at,
    -- Calculate days until auto-approval (for PENDING status)
    CASE
        WHEN cl.status = 'PENDING' THEN
            30 - EXTRACT(DAY FROM (CURRENT_TIMESTAMP - cl.created_at))::INTEGER
        ELSE NULL
    END AS days_until_approval,
    -- Verification status
    referrals.verify_ledger_integrity(cl.id) AS integrity_verified
FROM referrals.commission_ledger cl
JOIN properties.properties p ON p.id = cl.property_id
ORDER BY cl.created_at DESC;

COMMENT ON VIEW referrals.v_affiliate_earnings_history IS
'Real-time commission history. Use with WHERE affiliate_id = ? filter. Includes integrity verification. Updated to remove booking references.';

-- =====================================================================
-- STEP 12: GRANT PERMISSIONS
-- =====================================================================

GRANT SELECT ON referrals.v_property_referral_stats TO andresvargas;
GRANT SELECT ON referrals.v_affiliate_earnings_history TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.generate_verification_hash TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.verify_ledger_integrity TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.auto_generate_verification_hash TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.enforce_ledger_immutability TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.audit_commission_status_change TO andresvargas;

COMMIT;

-- =====================================================================
-- SUMMARY OF CHANGES
-- =====================================================================
/*
REMOVED:
- commission_ledger.booking_id column
- commission_ledger.booking_amount column
- prevent_self_referral() function and trigger (was querying non-existent bookings.bookings table)
- All booking references from verification hash functions
- All booking references from views

UPDATED:
- generate_verification_hash() - removed booking_id and booking_amount parameters
- verify_ledger_integrity() - uses new hash function signature
- auto_generate_verification_hash() - calls hash function without booking params
- enforce_ledger_immutability() - removed booking_id checks
- audit_commission_status_change() - removed booking_id from logs
- v_property_referral_stats - removed booking_amount, uses commission_amount only
- v_affiliate_earnings_history - removed booking_id and booking_amount fields

IMPACT:
- Referral commissions now tracked by commission_amount only (direct value)
- No dependency on bookings bounded context
- Fraud detection still functional (other triggers remain)
- All existing commission records preserved
- Views and functions fully functional without booking references
*/

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- WARNING: Rolling back this migration will require restoring booking_id
-- and booking_amount columns which will be NULL for all existing records.
-- Consider data migration strategy before rollback.