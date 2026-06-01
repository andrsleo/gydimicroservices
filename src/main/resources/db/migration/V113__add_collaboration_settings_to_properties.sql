-- V113: Add collaboration settings to properties table
-- Part of: Creator Collaboration Marketplace (feature 001)

ALTER TABLE properties.properties
    ADD COLUMN accept_creator_collaborations BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN accepted_compensations        TEXT[]  NOT NULL DEFAULT '{}';

COMMENT ON COLUMN properties.properties.accept_creator_collaborations
    IS 'Whether this property is open to creator collaboration pitches';

COMMENT ON COLUMN properties.properties.accepted_compensations
    IS 'Compensation types accepted: free_stay, cash, hybrid, affiliate, experience_exchange';
