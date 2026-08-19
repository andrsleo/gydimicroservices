-- V78__create_booking_status_history.sql
-- Purpose: Create immutable audit trail for booking status changes
-- Author: Database Architect AI
-- Date: 2026-02-06
-- Business Context: Track all status transitions for compliance, debugging, and customer support

BEGIN;

-- ============================================================================
-- 1. CREATE BOOKING STATUS HISTORY TABLE
-- ============================================================================

CREATE TABLE bookings.booking_status_history (
    id BIGSERIAL PRIMARY KEY,

    booking_id BIGINT NOT NULL,

    -- Status Transition
    old_status VARCHAR(50),  -- NULL for initial record (creation)
    new_status VARCHAR(50) NOT NULL,

    -- Audit Information
    changed_by BIGINT,  -- User ID who triggered the change (NULL if automatic/scheduler)
    change_reason TEXT,  -- Optional reason/notes for the change
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE if changed by scheduler, FALSE if manual

    -- Timestamp
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_status_history_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings.booking(id)
        ON DELETE CASCADE,  -- If booking deleted, cascade delete history

    CONSTRAINT fk_status_history_user
        FOREIGN KEY (changed_by)
        REFERENCES users.users(id)
        ON DELETE SET NULL,  -- Preserve history even if user deleted

    -- Business Rule Constraints
    CONSTRAINT chk_status_history_status_values
        CHECK (
            (old_status IS NULL OR old_status IN ('REQUEST', 'RESERVED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED', 'DISPUTED'))
            AND new_status IN ('REQUEST', 'RESERVED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED', 'DISPUTED')
        )
);


-- ============================================================================
-- 2. CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Primary query pattern: get history for a specific booking
CREATE INDEX idx_status_history_booking_created
    ON bookings.booking_status_history(booking_id, created_at DESC);

-- Lookup by user (admin audit: who changed what)
CREATE INDEX idx_status_history_user
    ON bookings.booking_status_history(changed_by, created_at DESC)
    WHERE changed_by IS NOT NULL;

-- Analytics: find automatic vs manual changes
CREATE INDEX idx_status_history_automatic
    ON bookings.booking_status_history(is_automatic, created_at DESC);


-- ============================================================================
-- 3. CREATE TRIGGER TO AUTO-LOG STATUS CHANGES
-- ============================================================================

CREATE OR REPLACE FUNCTION bookings.log_booking_status_change()
RETURNS TRIGGER AS $$
BEGIN
    -- On INSERT (booking creation)
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO bookings.booking_status_history (
            booking_id,
            old_status,
            new_status,
            changed_by,
            is_automatic
        ) VALUES (
            NEW.id,
            NULL,  -- No previous status on creation
            NEW.status,
            NULL,  -- Initial creation has no user (could be system or user)
            FALSE
        );

    -- On UPDATE (status change)
    ELSIF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO bookings.booking_status_history (
            booking_id,
            old_status,
            new_status,
            changed_by,
            is_automatic
        ) VALUES (
            NEW.id,
            OLD.status,
            NEW.status,
            NULL,  -- Will be set by application layer if manual change
            FALSE   -- Will be updated by application layer if automatic
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_booking_log_status_change
    AFTER INSERT OR UPDATE ON bookings.booking
    FOR EACH ROW
    EXECUTE FUNCTION bookings.log_booking_status_change();


-- ============================================================================
-- 4. HELPER FUNCTION: MANUALLY LOG STATUS CHANGE WITH CONTEXT
-- ============================================================================

-- Application layer can call this function to log status changes with full context
CREATE OR REPLACE FUNCTION bookings.log_status_change_with_context(
    p_booking_id BIGINT,
    p_old_status VARCHAR(50),
    p_new_status VARCHAR(50),
    p_changed_by BIGINT,
    p_reason TEXT,
    p_is_automatic BOOLEAN DEFAULT FALSE
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO bookings.booking_status_history (
        booking_id,
        old_status,
        new_status,
        changed_by,
        change_reason,
        is_automatic
    ) VALUES (
        p_booking_id,
        p_old_status,
        p_new_status,
        p_changed_by,
        p_reason,
        p_is_automatic
    );
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 5. HELPER FUNCTION: GET BOOKING STATUS TIMELINE
-- ============================================================================

-- Retrieve full status timeline for a booking (useful for debugging/support)
CREATE OR REPLACE FUNCTION bookings.get_booking_status_timeline(p_booking_id BIGINT)
RETURNS TABLE (
    change_number INT,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_by_email VARCHAR(255),
    change_reason TEXT,
    is_automatic BOOLEAN,
    changed_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ROW_NUMBER() OVER (ORDER BY h.created_at) :: INT AS change_number,
        h.old_status,
        h.new_status,
        u.email AS changed_by_email,
        h.change_reason,
        h.is_automatic,
        h.created_at AS changed_at
    FROM bookings.booking_status_history h
    LEFT JOIN users.users u ON h.changed_by = u.id
    WHERE h.booking_id = p_booking_id
    ORDER BY h.created_at;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 6. POPULATE HISTORY FOR EXISTING BOOKINGS (MIGRATION DATA)
-- ============================================================================

-- Create initial history record for all existing bookings migrated from V77
INSERT INTO bookings.booking_status_history (
    booking_id,
    old_status,
    new_status,
    changed_by,
    is_automatic,
    created_at
)
SELECT
    id,
    NULL,  -- No previous status (initial creation)
    status,
    NULL,  -- Unknown who created (migrated data)
    FALSE,
    created_at  -- Use booking creation timestamp
FROM bookings.booking
WHERE NOT EXISTS (
    -- Avoid duplicates if migration runs multiple times
    SELECT 1
    FROM bookings.booking_status_history h
    WHERE h.booking_id = bookings.booking.id
);


-- ============================================================================
-- 7. ADD TABLE AND COLUMN COMMENTS (DOCUMENTATION)
-- ============================================================================

COMMENT ON TABLE bookings.booking_status_history IS
'Immutable audit trail of all booking status changes. Automatically populated by trigger on bookings.booking. Use for compliance, debugging, and customer support.';

COMMENT ON COLUMN bookings.booking_status_history.booking_id IS
'Reference to the booking that changed status.';

COMMENT ON COLUMN bookings.booking_status_history.old_status IS
'Previous status before the change. NULL for the initial creation record.';

COMMENT ON COLUMN bookings.booking_status_history.new_status IS
'New status after the change.';

COMMENT ON COLUMN bookings.booking_status_history.changed_by IS
'User ID who triggered the status change. NULL if automatic (scheduler) or system-initiated.';

COMMENT ON COLUMN bookings.booking_status_history.change_reason IS
'Optional human-readable reason for the status change. Useful for manual cancellations, disputes, etc.';

COMMENT ON COLUMN bookings.booking_status_history.is_automatic IS
'TRUE if status changed automatically by scheduler (e.g., check-in/check-out date triggers). FALSE if changed manually by user.';

COMMENT ON COLUMN bookings.booking_status_history.created_at IS
'Timestamp when the status change occurred. Immutable.';

COMMENT ON FUNCTION bookings.log_status_change_with_context IS
'Helper function to manually log status changes with full context from application layer. Use when trigger auto-logging is insufficient.';

COMMENT ON FUNCTION bookings.get_booking_status_timeline IS
'Retrieve complete status change timeline for a booking, including who made changes and when. Useful for admin/support tools.';


-- ============================================================================
-- 8. VALIDATION QUERY (RUN AFTER MIGRATION)
-- ============================================================================

DO $$
DECLARE
    booking_count BIGINT;
    history_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO booking_count FROM bookings.booking;
    SELECT COUNT(DISTINCT booking_id) INTO history_count FROM bookings.booking_status_history;

    IF booking_count != history_count THEN
        RAISE WARNING 'Some bookings may not have history records: % bookings vs % with history',
            booking_count, history_count;
    ELSE
        RAISE NOTICE 'Migration successful: All % bookings have initial history records', booking_count;
    END IF;
END $$;


COMMIT;

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Get status timeline for booking #123
-- SELECT * FROM bookings.get_booking_status_timeline(123);

-- Example 2: Manually log status change with reason
-- SELECT bookings.log_status_change_with_context(
--     p_booking_id := 123,
--     p_old_status := 'REQUEST',
--     p_new_status := 'CANCELLED',
--     p_changed_by := 456,  -- User ID
--     p_reason := 'Guest requested cancellation due to travel restrictions',
--     p_is_automatic := FALSE
-- );

-- Example 3: Find all automatic status changes in last 7 days
-- SELECT
--     b.id,
--     b.guest_name,
--     h.old_status,
--     h.new_status,
--     h.created_at
-- FROM bookings.booking_status_history h
-- JOIN bookings.booking b ON h.booking_id = b.id
-- WHERE h.is_automatic = TRUE
--   AND h.created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'
-- ORDER BY h.created_at DESC;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (IF NEEDED)
-- ============================================================================

-- To rollback this migration:
--
-- BEGIN;
--
-- DROP FUNCTION IF EXISTS bookings.get_booking_status_timeline(BIGINT);
-- DROP FUNCTION IF EXISTS bookings.log_status_change_with_context;
-- DROP FUNCTION IF EXISTS bookings.log_booking_status_change() CASCADE;
-- DROP TABLE IF EXISTS bookings.booking_status_history CASCADE;
--
-- COMMIT;
