-- V89: Add SEND_GYDI_COHOST intermediate status to property approval flow
-- This state is reached automatically when a DRAFT property's minimum conditions are met.
-- The host must add gydiproperties@gmail.com as a co-host on Airbnb before submitting for admin review.

ALTER TABLE properties.properties DROP CONSTRAINT IF EXISTS chk_status;

ALTER TABLE properties.properties ADD CONSTRAINT chk_status CHECK (
    status IN ('DRAFT', 'SEND_GYDI_COHOST', 'PENDING_APPROVAL', 'PUBLISHED', 'INACTIVE', 'DENY', 'DELETED')
);
