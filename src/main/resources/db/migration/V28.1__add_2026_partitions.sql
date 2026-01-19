-- V28.1__add_2026_partitions.sql
-- Purpose: Add partitions for 2026 to referral_clicks table
-- Author: Antigravity
-- Date: 2026-01-09

BEGIN;

-- January 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_01
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- February 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_02
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- March 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_03
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- April 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_04
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- May 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_05
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- June 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_06
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

-- July 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_07
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

-- August 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_08
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- September 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_09
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

-- October 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_10
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

-- November 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_11
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

-- December 2026
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2026_12
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

COMMIT;
