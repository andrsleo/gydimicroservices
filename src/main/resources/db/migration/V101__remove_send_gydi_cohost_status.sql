-- V101: Remove SEND_GYDI_COHOST from property status flow
-- The cohost step is no longer required. Properties that were waiting in SEND_GYDI_COHOST
-- are reverted to DRAFT so the host can re-save and auto-transition to PENDING_APPROVAL.

-- 1. Migrate any existing SEND_GYDI_COHOST rows back to DRAFT
UPDATE properties.properties
SET status = 'DRAFT',
    updated_at = NOW()
WHERE status = 'SEND_GYDI_COHOST';

-- 2. Drop the old constraint that included SEND_GYDI_COHOST
ALTER TABLE properties.properties DROP CONSTRAINT IF EXISTS chk_status;

-- 3. Add new constraint without SEND_GYDI_COHOST
ALTER TABLE properties.properties ADD CONSTRAINT chk_status CHECK (
    status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'INACTIVE', 'DENY', 'DELETED')
);
