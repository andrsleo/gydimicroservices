-- V79__create_commissions_schema.sql
-- Purpose: Create commissions bounded context with separate tables for host (platform charges) and affiliate (platform pays)
-- Author: Database Architect AI
-- Date: 2026-02-06
-- Business Context: Platform earns profit = (host commission charged - affiliate commission paid)

BEGIN;

-- ============================================================================
-- 1. CREATE COMMISSIONS SCHEMA
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS commissions;

COMMENT ON SCHEMA commissions IS
'Bounded context for commission management. Platform charges hosts (host_commission) and pays affiliates (affiliate_commission) for successful referrals.';


-- ============================================================================
-- 2. CREATE HOST COMMISSION TABLE (PLATFORM CHARGES HOST)
-- ============================================================================

CREATE TABLE commissions.host_commission (
    id BIGSERIAL PRIMARY KEY,

    -- Relationships
    booking_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,

    -- Commission Calculation (snapshot at time of booking completion)
    host_plan VARCHAR(50) NOT NULL,  -- FREE, PRO, ELITE (at time of FINISHED)
    commission_rate NUMERIC(5,2) NOT NULL,  -- 0.25 (FREE), 0.20 (PRO), 0.15 (ELITE)

    -- Amounts
    booking_amount NUMERIC(10,2) NOT NULL,  -- Total booking amount
    commission_amount NUMERIC(10,2) NOT NULL,  -- Amount charged to host
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Stripe Payment Processing
    stripe_payment_intent_id VARCHAR(255),  -- Stripe PaymentIntent ID
    stripe_charge_id VARCHAR(255),  -- Stripe Charge ID (after capture)

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    charged_at TIMESTAMP WITH TIME ZONE,

    -- Error Handling (retry logic)
    failure_reason TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_host_commission_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings.booking(id)
        ON DELETE CASCADE,  -- If booking deleted, cascade delete commission

    CONSTRAINT fk_host_commission_host
        FOREIGN KEY (host_id)
        REFERENCES users.users(id)
        ON DELETE RESTRICT,  -- Preserve financial records even if user deleted

    -- Business Rule Constraints
    CONSTRAINT chk_host_commission_rate
        CHECK (commission_rate BETWEEN 0.10 AND 0.30),

    CONSTRAINT chk_host_commission_amounts
        CHECK (booking_amount > 0 AND commission_amount > 0),

    CONSTRAINT chk_host_commission_calculation
        CHECK (commission_amount <= booking_amount),  -- Commission can't exceed booking

    CONSTRAINT chk_host_commission_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'CHARGED', 'FAILED', 'REFUNDED', 'DISPUTED')),

    CONSTRAINT chk_host_commission_currency
        CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN')),

    CONSTRAINT chk_host_commission_plan
        CHECK (host_plan IN ('FREE', 'PRO', 'ELITE')),

    -- Ensure only one commission per booking
    CONSTRAINT uq_host_commission_booking
        UNIQUE (booking_id)
);


-- ============================================================================
-- 3. CREATE AFFILIATE COMMISSION TABLE (PLATFORM PAYS AFFILIATE)
-- ============================================================================

CREATE TABLE commissions.affiliate_commission (
    id BIGSERIAL PRIMARY KEY,

    -- Relationships
    booking_id BIGINT NOT NULL,
    affiliate_id BIGINT NOT NULL,

    -- Commission Calculation (snapshot at time of booking completion)
    affiliate_plan VARCHAR(50) NOT NULL,  -- FREE, PRO, ELITE (at time of FINISHED)
    commission_rate NUMERIC(5,2) NOT NULL,  -- 0.02 (FREE), 0.05 (PRO), 0.10 (ELITE)

    -- Amounts
    booking_amount NUMERIC(10,2) NOT NULL,  -- Total booking amount (same as host_commission)
    commission_amount NUMERIC(10,2) NOT NULL,  -- Amount to pay affiliate
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Payment Scheduling (batch payments on 1st and 15th of month)
    scheduled_payment_date DATE NOT NULL,  -- Calculated: next 1st or 15th after dispute period ends

    -- Stripe Payout Processing
    stripe_transfer_id VARCHAR(255),  -- Stripe Transfer ID (when paid)
    stripe_payout_id VARCHAR(255),  -- Stripe Payout ID (batch payout reference)

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Dispute Protection (7-day hold period after booking FINISHED)
    dispute_period_ends_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- booking.finished_at + 7 days
    -- Note: Check (dispute_period_ends_at > CURRENT_TIMESTAMP) in queries to determine if dispute period is active

    -- Error Handling
    failure_reason TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_affiliate_commission_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings.booking(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_affiliate_commission_affiliate
        FOREIGN KEY (affiliate_id)
        REFERENCES users.users(id)
        ON DELETE RESTRICT,

    -- Business Rule Constraints
    CONSTRAINT chk_affiliate_commission_rate
        CHECK (commission_rate BETWEEN 0.01 AND 0.15),

    CONSTRAINT chk_affiliate_commission_amounts
        CHECK (booking_amount > 0 AND commission_amount > 0),

    CONSTRAINT chk_affiliate_commission_calculation
        CHECK (commission_amount <= booking_amount),

    CONSTRAINT chk_affiliate_commission_status
        CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'CANCELLED', 'WITHHELD', 'DISPUTED')),

    CONSTRAINT chk_affiliate_commission_currency
        CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN')),

    CONSTRAINT chk_affiliate_commission_plan
        CHECK (affiliate_plan IN ('FREE', 'PRO', 'ELITE')),

    -- Ensure only one commission per booking
    CONSTRAINT uq_affiliate_commission_booking
        UNIQUE (booking_id)
);


-- ============================================================================
-- 4. CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- HOST COMMISSION INDEXES
CREATE INDEX idx_host_commission_booking
    ON commissions.host_commission(booking_id);

CREATE INDEX idx_host_commission_host_status
    ON commissions.host_commission(host_id, status, created_at DESC);

CREATE INDEX idx_host_commission_status_created
    ON commissions.host_commission(status, created_at DESC);

-- Retry queue: find failed commissions to retry
CREATE INDEX idx_host_commission_retry_queue
    ON commissions.host_commission(next_retry_at, status)
    WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;

-- AFFILIATE COMMISSION INDEXES
CREATE INDEX idx_affiliate_commission_booking
    ON commissions.affiliate_commission(booking_id);

CREATE INDEX idx_affiliate_commission_affiliate_status
    ON commissions.affiliate_commission(affiliate_id, status, created_at DESC);

CREATE INDEX idx_affiliate_commission_status_created
    ON commissions.affiliate_commission(status, created_at DESC);

-- Payment batch processing: find commissions ready to pay
CREATE INDEX idx_affiliate_commission_payment_queue
    ON commissions.affiliate_commission(scheduled_payment_date, status, dispute_period_ends_at)
    WHERE status = 'APPROVED';

-- Dispute monitoring: find commissions in dispute period
CREATE INDEX idx_affiliate_commission_dispute_period
    ON commissions.affiliate_commission(dispute_period_ends_at, status)
    WHERE status IN ('PENDING', 'APPROVED');


-- ============================================================================
-- 5. CREATE UPDATED_AT TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION commissions.update_commission_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_host_commission_updated_at
    BEFORE UPDATE ON commissions.host_commission
    FOR EACH ROW
    EXECUTE FUNCTION commissions.update_commission_updated_at();

CREATE TRIGGER tr_affiliate_commission_updated_at
    BEFORE UPDATE ON commissions.affiliate_commission
    FOR EACH ROW
    EXECUTE FUNCTION commissions.update_commission_updated_at();


-- ============================================================================
-- 6. HELPER FUNCTIONS
-- ============================================================================

-- Calculate next payment date (1st or 15th of the month)
CREATE OR REPLACE FUNCTION commissions.calculate_next_payment_date(
    p_dispute_period_ends_at TIMESTAMP WITH TIME ZONE
)
RETURNS DATE AS $$
DECLARE
    v_base_date DATE;
    v_day INT;
    v_result DATE;
BEGIN
    v_base_date := p_dispute_period_ends_at::DATE;
    v_day := EXTRACT(DAY FROM v_base_date);

    -- If before the 1st, schedule for 1st of current month
    IF v_day < 1 THEN
        v_result := DATE_TRUNC('month', v_base_date)::DATE;

    -- If between 1st and 15th, schedule for 15th of current month
    ELSIF v_day < 15 THEN
        v_result := DATE_TRUNC('month', v_base_date)::DATE + INTERVAL '14 days';

    -- If after 15th, schedule for 1st of next month
    ELSE
        v_result := (DATE_TRUNC('month', v_base_date) + INTERVAL '1 month')::DATE;
    END IF;

    RETURN v_result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;


-- Get commission summary for a user (dashboard stats)
CREATE OR REPLACE FUNCTION commissions.get_affiliate_commission_summary(
    p_affiliate_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
)
RETURNS TABLE (
    total_bookings BIGINT,
    total_commission_amount NUMERIC,
    pending_amount NUMERIC,
    approved_amount NUMERIC,
    paid_amount NUMERIC,
    withheld_amount NUMERIC,
    currency VARCHAR(3)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_bookings,
        COALESCE(SUM(commission_amount), 0) AS total_commission_amount,
        COALESCE(SUM(CASE WHEN status = 'PENDING' THEN commission_amount ELSE 0 END), 0) AS pending_amount,
        COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN commission_amount ELSE 0 END), 0) AS approved_amount,
        COALESCE(SUM(CASE WHEN status = 'PAID' THEN commission_amount ELSE 0 END), 0) AS paid_amount,
        COALESCE(SUM(CASE WHEN status = 'WITHHELD' THEN commission_amount ELSE 0 END), 0) AS withheld_amount,
        MAX(ac.currency) AS currency
    FROM commissions.affiliate_commission ac
    WHERE ac.affiliate_id = p_affiliate_id
      AND (p_start_date IS NULL OR ac.created_at::DATE >= p_start_date)
      AND (p_end_date IS NULL OR ac.created_at::DATE <= p_end_date);
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 7. ADD TABLE AND COLUMN COMMENTS (DOCUMENTATION)
-- ============================================================================

COMMENT ON TABLE commissions.host_commission IS
'Platform charges hosts when bookings are completed. Charged immediately when booking status = FINISHED. Platform revenue stream.';

COMMENT ON TABLE commissions.affiliate_commission IS
'Platform pays affiliates for successful referrals. Paid in batches on 1st and 15th of each month after 7-day dispute period. Platform expense.';

COMMENT ON COLUMN commissions.host_commission.commission_rate IS
'Platform fee charged to host: FREE=25%, PRO=20%, ELITE=15%. Snapshot at time of booking completion.';

COMMENT ON COLUMN commissions.host_commission.commission_amount IS
'Amount charged to host (booking_amount × commission_rate). Platform revenue.';

COMMENT ON COLUMN commissions.host_commission.status IS
'PENDING: Not yet charged. PROCESSING: Stripe charge in progress. CHARGED: Successfully charged. FAILED: Charge failed (will retry). REFUNDED: Charge refunded. DISPUTED: Guest disputed the booking.';

COMMENT ON COLUMN commissions.affiliate_commission.commission_rate IS
'Commission paid to affiliate: FREE=2%, PRO=5%, ELITE=10%. Snapshot at time of booking completion.';

COMMENT ON COLUMN commissions.affiliate_commission.commission_amount IS
'Amount paid to affiliate (booking_amount × commission_rate). Platform expense.';

COMMENT ON COLUMN commissions.affiliate_commission.scheduled_payment_date IS
'Date when commission will be paid (1st or 15th of month after dispute period ends).';

COMMENT ON COLUMN commissions.affiliate_commission.dispute_period_ends_at IS
'End of 7-day dispute protection period (booking.finished_at + 7 days). Commission cannot be paid until this date.';

COMMENT ON COLUMN commissions.affiliate_commission.status IS
'PENDING: In dispute period. APPROVED: Ready to pay (dispute period ended). PAID: Successfully paid. CANCELLED: Booking cancelled/disputed. WITHHELD: Payment held due to investigation. DISPUTED: Guest disputed booking.';

COMMENT ON FUNCTION commissions.calculate_next_payment_date IS
'Calculate next payment date (1st or 15th of month) after dispute period ends. Used when creating affiliate commission records.';

COMMENT ON FUNCTION commissions.get_affiliate_commission_summary IS
'Get commission summary for affiliate dashboard. Returns total bookings, amounts by status, and currency.';


-- ============================================================================
-- 8. DATA MIGRATION (IF NEEDED)
-- ============================================================================

-- NOTE: referrals.commission_booking is expected to be empty in MVP
-- If data exists, uncomment and adjust the migration below:

-- Migrate host commissions (if data exists)
-- INSERT INTO commissions.host_commission (
--     booking_id,
--     host_id,
--     host_plan,
--     commission_rate,
--     booking_amount,
--     commission_amount,
--     currency,
--     status,
--     created_at,
--     updated_at
-- )
-- SELECT
--     cb.booking_id,
--     b.property.owner_id,  -- Adjust based on actual schema
--     COALESCE(cb.affiliate_plan, 'FREE'),  -- Assuming plan was stored
--     cb.commission_rate,
--     cb.commission_amount / cb.commission_rate,  -- Reverse calculate booking amount
--     cb.commission_amount,
--     'USD',
--     CASE
--         WHEN cb.paid_at IS NOT NULL THEN 'CHARGED'
--         WHEN cb.status = 'APPROVED' THEN 'PENDING'
--         ELSE 'PENDING'
--     END,
--     cb.created_at,
--     cb.updated_at
-- FROM referrals.commission_booking cb
-- JOIN bookings.booking b ON cb.booking_id = b.id
-- WHERE cb.commission_amount > 0;  -- Only positive commissions

-- For now, we assume commission_booking is empty (MVP)
-- Verify:
DO $$
DECLARE
    old_commission_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO old_commission_count FROM referrals.commission_booking;

    IF old_commission_count > 0 THEN
        RAISE WARNING 'Found % existing commission records in referrals.commission_booking. Manual data migration may be required.', old_commission_count;
    ELSE
        RAISE NOTICE 'No existing commission data to migrate. Tables created successfully.';
    END IF;
END $$;


-- ============================================================================
-- 9. DEPRECATE OLD TABLE
-- ============================================================================

COMMENT ON TABLE referrals.commission_booking IS
'⚠️ DEPRECATED: Replaced by commissions.host_commission and commissions.affiliate_commission in V79. This table will be dropped in a future migration. DO NOT USE.';


COMMIT;

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Create host commission when booking is FINISHED
-- INSERT INTO commissions.host_commission (
--     booking_id, host_id, host_plan, commission_rate, booking_amount, commission_amount
-- ) VALUES (
--     123, 456, 'PRO', 0.20, 1000.00, 200.00
-- );

-- Example 2: Create affiliate commission with dispute period
-- INSERT INTO commissions.affiliate_commission (
--     booking_id, affiliate_id, affiliate_plan, commission_rate,
--     booking_amount, commission_amount,
--     dispute_period_ends_at,
--     scheduled_payment_date
-- ) VALUES (
--     123, 789, 'PRO', 0.05, 1000.00, 50.00,
--     CURRENT_TIMESTAMP + INTERVAL '7 days',
--     commissions.calculate_next_payment_date(CURRENT_TIMESTAMP + INTERVAL '7 days')
-- );

-- Example 3: Get affiliate commission summary
-- SELECT * FROM commissions.get_affiliate_commission_summary(
--     p_affiliate_id := 789,
--     p_start_date := '2026-01-01',
--     p_end_date := '2026-12-31'
-- );

-- Example 4: Find commissions ready to pay on next payment date
-- SELECT
--     ac.id,
--     ac.affiliate_id,
--     ac.commission_amount,
--     ac.scheduled_payment_date
-- FROM commissions.affiliate_commission ac
-- WHERE ac.status = 'APPROVED'
--   AND ac.dispute_period_ends_at <= CURRENT_TIMESTAMP
--   AND ac.scheduled_payment_date <= CURRENT_DATE
-- ORDER BY ac.scheduled_payment_date, ac.created_at;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (IF NEEDED)
-- ============================================================================

-- To rollback this migration:
--
-- BEGIN;
--
-- DROP FUNCTION IF EXISTS commissions.get_affiliate_commission_summary;
-- DROP FUNCTION IF EXISTS commissions.calculate_next_payment_date;
-- DROP FUNCTION IF EXISTS commissions.update_commission_updated_at() CASCADE;
-- DROP TABLE IF EXISTS commissions.affiliate_commission CASCADE;
-- DROP TABLE IF EXISTS commissions.host_commission CASCADE;
-- DROP SCHEMA IF EXISTS commissions CASCADE;
--
-- -- Remove deprecation comment
-- COMMENT ON TABLE referrals.commission_booking IS 'Commission records for bookings.';
--
-- COMMIT;
