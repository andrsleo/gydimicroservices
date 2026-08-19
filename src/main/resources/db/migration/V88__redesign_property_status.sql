-- V88: Redesign property status to include admin approval flow
-- Adds PENDING_APPROVAL and DENY states, plus audit timestamp columns

-- Drop existing status constraint
ALTER TABLE properties.properties DROP CONSTRAINT IF EXISTS chk_status;

-- Add new constraint with all 6 states
ALTER TABLE properties.properties ADD CONSTRAINT chk_status CHECK (
    status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'INACTIVE', 'DENY', 'DELETED')
);

-- Add new audit columns for approval workflow
ALTER TABLE properties.properties ADD COLUMN IF NOT EXISTS denial_reason TEXT;
ALTER TABLE properties.properties ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE properties.properties ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE properties.properties ADD COLUMN IF NOT EXISTS denied_at TIMESTAMP;

-- Index for fast admin pending list queries
CREATE INDEX IF NOT EXISTS idx_properties_pending_approval
    ON properties.properties(status) WHERE status = 'PENDING_APPROVAL';
