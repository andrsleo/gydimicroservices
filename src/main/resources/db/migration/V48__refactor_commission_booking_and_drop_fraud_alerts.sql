-- V48__refactor_commission_booking_and_drop_fraud_alerts.sql
-- Purpose: Refactor commission_ledger to commission_booking and drop fraud_alerts table
-- Author: Database Architect AI
-- Date: 2025-12-05
-- Changes:
--   1. Drop fraud_alerts table completely
--   2. Refactor commission_ledger -> commission_booking with booking_id FK
--   3. Update functions, triggers, and indexes

BEGIN;

-- =====================================================================
-- PART 1: DROP FRAUD_ALERTS TABLE AND ALL DEPENDENCIES
-- =====================================================================

-- Drop materialized views that depend on fraud_alerts
DROP MATERIALIZED VIEW IF EXISTS referrals.mv_fraud_metrics CASCADE;

-- Drop triggers on fraud_alerts
DROP TRIGGER IF EXISTS tr_auto_investigate_high_fraud ON referrals.fraud_alerts CASCADE;
DROP TRIGGER IF EXISTS tr_fraud_alerts_updated_at ON referrals.fraud_alerts CASCADE;

-- Drop functions related to fraud_alerts
DROP FUNCTION IF EXISTS referrals.get_affiliate_fraud_risk(BIGINT) CASCADE;
DROP FUNCTION IF EXISTS referrals.create_fraud_alert(
    VARCHAR, referrals.fraud_severity, INTEGER, BIGINT, BIGINT, BIGINT, JSONB, VARCHAR
) CASCADE;
DROP FUNCTION IF EXISTS referrals.auto_investigate_high_fraud() CASCADE;

-- Drop indexes on fraud_alerts (if not automatically dropped)
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_unresolved CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_affiliate CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_type_severity CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_assigned CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_high_risk CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_evidence CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_dashboard_type CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_affiliate_reputation CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_link_investigation CASCADE;
DROP INDEX IF EXISTS referrals.idx_fraud_alerts_workload CASCADE;

-- Drop the fraud_alerts table
DROP TABLE IF EXISTS referrals.fraud_alerts CASCADE;

-- Drop fraud-related enums
DROP TYPE IF EXISTS referrals.fraud_severity CASCADE;
DROP TYPE IF EXISTS referrals.fraud_alert_status CASCADE;

-- =====================================================================
-- PART 2: REFACTOR COMMISSION_LEDGER -> COMMISSION_BOOKING
-- =====================================================================

-- Step 2.1: Drop existing triggers on commission_ledger
DROP TRIGGER IF EXISTS tr_prevent_ledger_delete ON referrals.commission_ledger CASCADE;
DROP TRIGGER IF EXISTS tr_audit_commission_status_change ON referrals.commission_ledger CASCADE;
DROP TRIGGER IF EXISTS tr_enforce_ledger_immutability ON referrals.commission_ledger CASCADE;
DROP TRIGGER IF EXISTS tr_update_conversion_metrics ON referrals.commission_ledger CASCADE;
DROP TRIGGER IF EXISTS tr_auto_generate_verification_hash ON referrals.commission_ledger CASCADE;
DROP TRIGGER IF EXISTS tr_commission_ledger_updated_at ON referrals.commission_ledger CASCADE;

-- Step 2.2: Drop existing indexes on commission_ledger
DROP INDEX IF EXISTS referrals.idx_commission_ledger_affiliate_status CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_approved CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_pending_review CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_link CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_property CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_verification CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_history CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_sum CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_property_performance CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_plan_analytics CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_payout_batch CASCADE;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_link_join CASCADE;

-- Step 2.3: Drop old functions (will recreate new versions)
DROP FUNCTION IF EXISTS referrals.auto_approve_pending_commissions() CASCADE;
DROP FUNCTION IF EXISTS referrals.verify_ledger_integrity(BIGINT) CASCADE;
DROP FUNCTION IF EXISTS referrals.generate_verification_hash(
    BIGINT, BIGINT, BIGINT, BIGINT, DECIMAL, DECIMAL, VARCHAR
) CASCADE;
DROP FUNCTION IF EXISTS referrals.prevent_ledger_delete() CASCADE;
DROP FUNCTION IF EXISTS referrals.audit_commission_status_change() CASCADE;
DROP FUNCTION IF EXISTS referrals.enforce_ledger_immutability() CASCADE;
DROP FUNCTION IF EXISTS referrals.update_conversion_metrics() CASCADE;
DROP FUNCTION IF EXISTS referrals.auto_generate_verification_hash() CASCADE;

-- Step 2.4: Drop existing materialized/regular views that depend on commission_ledger
DROP MATERIALIZED VIEW IF EXISTS referrals.mv_affiliate_performance CASCADE;
DROP VIEW IF EXISTS referrals.v_affiliate_earnings_history CASCADE;
DROP VIEW IF EXISTS referrals.v_pending_payouts CASCADE;
DROP VIEW IF EXISTS referrals.v_property_referral_stats CASCADE;
DROP VIEW IF EXISTS referrals.v_top_links CASCADE;
DROP FUNCTION IF EXISTS referrals.refresh_all_materialized_views() CASCADE;
DROP FUNCTION IF EXISTS referrals.refresh_fraud_metrics() CASCADE;
DROP FUNCTION IF EXISTS referrals.refresh_affiliate_performance() CASCADE;

-- Step 2.5: Add new booking_id column (initially nullable for migration)
ALTER TABLE referrals.commission_ledger
ADD COLUMN IF NOT EXISTS booking_id BIGINT;

-- Step 2.6: Drop old foreign key constraints
ALTER TABLE referrals.commission_ledger
DROP CONSTRAINT IF EXISTS fk_commission_ledger_referral_link CASCADE;

ALTER TABLE referrals.commission_ledger
DROP CONSTRAINT IF EXISTS fk_commission_ledger_affiliate CASCADE;

ALTER TABLE referrals.commission_ledger
DROP CONSTRAINT IF EXISTS fk_commission_ledger_property CASCADE;

-- Step 2.7: Drop old columns (no data exists, safe to drop)
ALTER TABLE referrals.commission_ledger
DROP COLUMN IF EXISTS referral_link_id CASCADE;

ALTER TABLE referrals.commission_ledger
DROP COLUMN IF EXISTS affiliate_id CASCADE;

ALTER TABLE referrals.commission_ledger
DROP COLUMN IF EXISTS property_id CASCADE;

-- Step 2.8: Make booking_id NOT NULL and add foreign key
ALTER TABLE referrals.commission_ledger
ALTER COLUMN booking_id SET NOT NULL;

ALTER TABLE referrals.commission_ledger
ADD CONSTRAINT fk_commission_booking_booking
    FOREIGN KEY (booking_id)
    REFERENCES referrals.booking(booking_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- Step 2.9: Rename the table
ALTER TABLE referrals.commission_ledger
RENAME TO commission_booking;

-- =====================================================================
-- PART 3: RECREATE FUNCTIONS WITH NEW SCHEMA
-- =====================================================================

-- Function: Generate verification hash (NEW VERSION with booking_id)
CREATE OR REPLACE FUNCTION referrals.generate_verification_hash(
    p_id BIGINT,
    p_booking_id BIGINT,
    p_commission_rate DECIMAL,
    p_commission_amount DECIMAL,
    p_affiliate_plan VARCHAR
) RETURNS BYTEA AS $$
DECLARE
    hash_input TEXT;
BEGIN
    -- Concatenate all immutable fields
    hash_input := CONCAT(
        p_id::TEXT, '|',
        p_booking_id::TEXT, '|',
        p_commission_rate::TEXT, '|',
        p_commission_amount::TEXT, '|',
        p_affiliate_plan
    );

    -- Return SHA-256 hash
    RETURN digest(hash_input, 'sha256');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function: Verify ledger integrity (UPDATED for new schema)
CREATE OR REPLACE FUNCTION referrals.verify_ledger_integrity(
    p_commission_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
    record RECORD;
    expected_hash BYTEA;
BEGIN
    -- Fetch record
    SELECT * INTO record
    FROM referrals.commission_booking
    WHERE id = p_commission_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Calculate expected hash
    expected_hash := referrals.generate_verification_hash(
        record.id,
        record.booking_id,
        record.commission_rate,
        record.commission_amount,
        record.affiliate_plan
    );

    -- Compare hashes
    RETURN record.verification_hash = expected_hash;
END;
$$ LANGUAGE plpgsql;

-- Function: Auto-approve pending commissions (UPDATED for new table name)
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
        UPDATE referrals.commission_booking
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

-- Function: Auto-generate verification hash on INSERT (NEW VERSION)
CREATE OR REPLACE FUNCTION referrals.auto_generate_verification_hash()
RETURNS TRIGGER AS $$
BEGIN
    -- Generate verification hash automatically
    NEW.verification_hash := referrals.generate_verification_hash(
        NEW.id,
        NEW.booking_id,
        NEW.commission_rate,
        NEW.commission_amount,
        NEW.affiliate_plan
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function: Update updated_at timestamp
CREATE OR REPLACE FUNCTION referrals.update_commission_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- PART 4: CREATE NEW INDEXES
-- =====================================================================

-- Index for fetching commissions by booking
CREATE INDEX idx_commission_booking_booking_id
ON referrals.commission_booking(booking_id);

-- Index for payout processing (fetch all APPROVED records)
CREATE INDEX idx_commission_booking_approved
ON referrals.commission_booking(status, approved_at)
WHERE status = 'APPROVED';

-- Index for pending fraud review (auto-approve after 30 days)
CREATE INDEX idx_commission_booking_pending_review
ON referrals.commission_booking(status, created_at)
WHERE status = 'PENDING';

-- Index for verification hash lookup (tamper detection)
CREATE INDEX idx_commission_booking_verification
ON referrals.commission_booking(verification_hash);

-- Index for status filtering and ordering
CREATE INDEX idx_commission_booking_status_created
ON referrals.commission_booking(status, created_at DESC);

-- =====================================================================
-- PART 5: CREATE TRIGGERS
-- =====================================================================

-- Trigger: Auto-generate verification hash on INSERT
CREATE TRIGGER tr_auto_generate_verification_hash
    BEFORE INSERT ON referrals.commission_booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.auto_generate_verification_hash();

-- Trigger: Update updated_at on UPDATE
CREATE TRIGGER tr_commission_booking_updated_at
    BEFORE UPDATE ON referrals.commission_booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.update_commission_booking_updated_at();

-- =====================================================================
-- PART 6: ADD COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON TABLE referrals.commission_booking IS
'Commission tracking table linked to bookings. Records are immutable after creation (enforced by trigger). Lifecycle: PENDING (30 days) → APPROVED → PAID.';

COMMENT ON COLUMN referrals.commission_booking.id IS
'Primary key, auto-incrementing commission identifier';

COMMENT ON COLUMN referrals.commission_booking.booking_id IS
'Foreign key to referrals.booking table. Links commission to specific booking.';

COMMENT ON COLUMN referrals.commission_booking.commission_rate IS
'Commission rate as decimal (0.02 = 2%, 0.05 = 5%, 0.15 = 15%). Immutable snapshot of affiliate plan rate at conversion time.';

COMMENT ON COLUMN referrals.commission_booking.commission_amount IS
'Commission amount in USD. Represents the actual commission earned by the affiliate for this booking.';

COMMENT ON COLUMN referrals.commission_booking.affiliate_plan IS
'Snapshot of affiliate subscription plan at conversion time (FREE/PRO/ELITE). Immutable for audit purposes.';

COMMENT ON COLUMN referrals.commission_booking.status IS
'Lifecycle workflow: PENDING (0-30 days fraud review) → APPROVED (ready for payout) → PAID (payment processed). Can also be REJECTED (fraud) or REFUNDED (transaction refunded).';

COMMENT ON COLUMN referrals.commission_booking.verification_hash IS
'SHA-256 hash of all immutable fields. Used for tamper detection. Generated automatically by trigger on INSERT.';

COMMENT ON COLUMN referrals.commission_booking.approved_at IS
'Timestamp when status changed to APPROVED (after 30-day fraud window). Set automatically by trigger or manual approval.';

COMMENT ON COLUMN referrals.commission_booking.paid_at IS
'Timestamp when payment was processed to affiliate bank account. Set by payment processing system.';

COMMENT ON FUNCTION referrals.generate_verification_hash IS
'Generates SHA-256 hash of all immutable ledger fields for tamper detection. NEW VERSION uses booking_id instead of referral_link_id, affiliate_id, property_id.';

COMMENT ON FUNCTION referrals.verify_ledger_integrity IS
'Verifies commission record integrity by comparing stored hash with recalculated hash. Returns FALSE if tampering detected.';

COMMENT ON FUNCTION referrals.auto_approve_pending_commissions IS
'Auto-approves commissions pending longer than 30 days (fraud window). Run daily: SELECT * FROM referrals.auto_approve_pending_commissions();';

-- =====================================================================
-- PART 7: GRANT PERMISSIONS
-- =====================================================================



COMMIT;

-- =====================================================================
-- ROLLBACK INSTRUCTIONS (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
/*
To rollback this migration (manually):

1. Restore fraud_alerts table:
   - Re-run migration V30__create_fraud_alerts_table.sql

2. Restore commission_ledger original schema:
   - Rename table back: ALTER TABLE referrals.commission_booking RENAME TO commission_ledger;
   - Add back columns: referral_link_id, affiliate_id, property_id
   - Drop booking_id column
   - Recreate original functions and triggers
   - Re-run migration V29__create_commission_ledger_table.sql

Note: This is a destructive operation for fraud_alerts. All fraud data will be lost.
*/

-- =====================================================================
-- POST-MIGRATION TASKS
-- =====================================================================
/*
After applying this migration:

1. UPDATE JAVA CODE:
   - Update CommissionJpaEntity.java to use bookingId instead of referralLinkId, affiliateId, propertyId
   - Update Commission.java domain model
   - Update CommissionRepository and adapter
   - Update any DTOs and mappers

2. DELETE FRAUD-RELATED JAVA FILES:
   - FraudAlertStatus.java (enum)
   - FraudSeverity.java (enum)
   - Any fraud alert entities/repositories/services

3. VERIFY:
   - Run application: ./mvnw spring-boot:run
   - Check for missing bean errors
   - Run all tests: ./mvnw test
   - Verify Flyway applied successfully: SELECT * FROM flyway_schema_history WHERE version = '48';

4. TEST:
   - Create a test booking
   - Verify commission_booking record is created with correct booking_id
   - Verify verification hash is generated correctly
   - Test auto_approve_pending_commissions() function
*/
