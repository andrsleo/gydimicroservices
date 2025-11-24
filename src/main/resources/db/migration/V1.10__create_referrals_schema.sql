-- V1.10__create_referrals_schema.sql
-- Purpose: Create referrals schema and enums for referral system
-- Author: Database Architect
-- Date: 2025-11-12

BEGIN;

-- Create referrals schema
CREATE SCHEMA IF NOT EXISTS referrals;

-- Grant usage to application user
GRANT USAGE ON SCHEMA referrals TO andresvargas;

-- Create custom types for type safety
CREATE TYPE referrals.referral_link_status AS ENUM (
    'ACTIVE',
    'PAUSED',
    'EXPIRED',
    'DISABLED'
);

CREATE TYPE referrals.commission_status AS ENUM (
    'PENDING',      -- Initial state after conversion
    'APPROVED',     -- After fraud check passed (30 days)
    'PAID',         -- Payment processed
    'REJECTED',     -- Fraud detected
    'REFUNDED'      -- Transaction refunded
);

CREATE TYPE referrals.device_type AS ENUM (
    'DESKTOP',
    'MOBILE',
    'TABLET',
    'BOT',
    'UNKNOWN'
);

-- Create audit log table for security events
CREATE TABLE referrals.security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    ip_address_hash BYTEA,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for audit log queries
CREATE INDEX idx_security_audit_log_event_type_created_at
ON referrals.security_audit_log(event_type, created_at DESC);

-- Grant permissions
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA referrals TO andresvargas;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA referrals TO andresvargas;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA referrals
GRANT SELECT, INSERT ON TABLES TO andresvargas;

ALTER DEFAULT PRIVILEGES IN SCHEMA referrals
GRANT USAGE, SELECT ON SEQUENCES TO andresvargas;

COMMIT;

-- Rollback (for reference, not executed):
-- DROP SCHEMA IF EXISTS referrals CASCADE;