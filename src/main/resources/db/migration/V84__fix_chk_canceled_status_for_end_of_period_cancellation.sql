-- V84: Fix chk_canceled_status to allow end-of-period cancellations
--
-- Problem: Current constraint doesn't allow status=ACTIVE with canceled_at set,
-- which is needed for end-of-period cancellations where:
--   - User requests cancellation (canceled_at is set)
--   - Subscription remains ACTIVE until expiresAt
--   - Scheduler downgrades to FREE when expired
--
-- Current constraint (TOO RESTRICTIVE):
--   CHECK ((status = 'CANCELED' AND canceled_at IS NOT NULL) OR
--          (status != 'CANCELED' AND canceled_at IS NULL))
--
-- New constraint (ALLOWS END-OF-PERIOD):
--   CHECK (status != 'CANCELED' OR (status = 'CANCELED' AND canceled_at IS NOT NULL))
--   Meaning: If status is CANCELED, then canceled_at must be set.
--            If status is not CANCELED (ACTIVE/EXPIRED), canceled_at can be anything.

-- Drop the old constraint
ALTER TABLE subscriptions.user_subscriptions
    DROP CONSTRAINT IF EXISTS chk_canceled_status;

-- Add the new, less restrictive constraint
ALTER TABLE subscriptions.user_subscriptions
    ADD CONSTRAINT chk_canceled_status
    CHECK (
        -- If status is CANCELED, canceled_at must be set
        status != 'CANCELED' OR (status = 'CANCELED' AND canceled_at IS NOT NULL)
    );

-- Add comment explaining the constraint
COMMENT ON CONSTRAINT chk_canceled_status ON subscriptions.user_subscriptions IS
    'Ensures that if status is CANCELED, canceled_at must be set. Allows ACTIVE status with canceled_at for end-of-period cancellations.';
