-- V1.11__create_referral_links_table.sql
-- Purpose: Create referral_links table for tracking generated affiliate links
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.10 (referrals schema, ENUMs)

BEGIN;

-- =====================================================================
-- REFERRAL LINKS TABLE
-- =====================================================================
-- Stores all generated referral links by affiliates
-- Each link is unique per affiliate-property combination
-- Performance: ~10K links expected, indexed for fast lookups

CREATE TABLE IF NOT EXISTS referrals.referral_links (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    -- affiliate_id references users.users(id) which is BIGSERIAL
    -- property_id references properties.properties(id) which is BIGSERIAL
    affiliate_id BIGINT NOT NULL,  -- FK to users.users(id)
    property_id BIGINT NOT NULL,   -- FK to properties.properties(id)

    -- Security & tracking
    encrypted_token TEXT NOT NULL UNIQUE,  -- PASETO v4.local token
    short_code VARCHAR(10) UNIQUE,          -- Optional short URL (e.g., "aBc123Xyz")

    -- Performance counters (denormalized for fast dashboard queries)
    clicks_count INTEGER NOT NULL DEFAULT 0,
    conversions_count INTEGER NOT NULL DEFAULT 0,
    total_commission DECIMAL(12,2) NOT NULL DEFAULT 0.00,

    -- Status & lifecycle
    status referrals.referral_link_status NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Soft delete support
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_affiliate_property UNIQUE (affiliate_id, property_id, deleted_at),
    CONSTRAINT valid_counts CHECK (
        clicks_count >= 0 AND
        conversions_count >= 0 AND
        conversions_count <= clicks_count
    ),
    CONSTRAINT valid_commission CHECK (total_commission >= 0),

    -- Foreign key constraints
    CONSTRAINT fk_referral_links_affiliate FOREIGN KEY (affiliate_id)
        REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_links_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE
);

-- =====================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================

-- Index for affiliate dashboard (fetch active links by affiliate)
CREATE INDEX idx_referral_links_affiliate_active
ON referrals.referral_links(affiliate_id, created_at DESC)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Index for property detail page (fetch all links for a property)
CREATE INDEX idx_referral_links_property
ON referrals.referral_links(property_id, created_at DESC)
WHERE deleted_at IS NULL;

-- Index for token validation (fast lookup during click tracking)
CREATE INDEX idx_referral_links_token
ON referrals.referral_links(encrypted_token)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Index for short code resolution
CREATE INDEX idx_referral_links_short_code
ON referrals.referral_links(short_code)
WHERE short_code IS NOT NULL AND deleted_at IS NULL;

-- Index for expiration cleanup job
CREATE INDEX idx_referral_links_expired
ON referrals.referral_links(expires_at)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Composite index for performance reporting
CREATE INDEX idx_referral_links_performance
ON referrals.referral_links(affiliate_id, conversions_count DESC, total_commission DESC)
WHERE deleted_at IS NULL;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON TABLE referrals.referral_links IS
'Core table storing all generated referral links. Each affiliate can generate one link per property. Links are PASETO encrypted tokens containing affiliate_id and property_id.';

COMMENT ON COLUMN referrals.referral_links.affiliate_id IS
'FK to users.users(id) - References the user who owns this referral link.';

COMMENT ON COLUMN referrals.referral_links.property_id IS
'FK to properties.properties(id) - References the property being promoted by this referral link.';

COMMENT ON COLUMN referrals.referral_links.encrypted_token IS
'PASETO v4.local token containing encrypted payload: {affiliate_id, property_id, timestamp}. Token is cryptographically signed to prevent tampering.';

COMMENT ON COLUMN referrals.referral_links.short_code IS
'Optional short URL code (e.g., gydi.com/r/aBc123Xyz). Generated using base62 encoding. Improves shareability in social media.';

COMMENT ON COLUMN referrals.referral_links.clicks_count IS
'Denormalized counter incremented by trigger on referral_clicks INSERT. Enables fast dashboard queries without JOINs.';

COMMENT ON COLUMN referrals.referral_links.conversions_count IS
'Denormalized counter incremented by trigger on commission_ledger INSERT. Tracks successful bookings from this link.';

COMMENT ON COLUMN referrals.referral_links.total_commission IS
'Denormalized SUM of all commission_amount from commission_ledger. Updated by trigger. Enables instant earnings display.';

COMMENT ON COLUMN referrals.referral_links.status IS
'Link lifecycle: ACTIVE (accepting clicks), PAUSED (temporarily disabled by affiliate), EXPIRED (past expires_at), DISABLED (admin action).';

COMMENT ON COLUMN referrals.referral_links.deleted_at IS
'Soft delete timestamp. Enables unique constraint to allow re-creating affiliate-property combinations after deletion.';

-- =====================================================================
-- GRANT PERMISSIONS
-- =====================================================================



COMMIT;

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP TABLE IF EXISTS referrals.referral_links CASCADE;