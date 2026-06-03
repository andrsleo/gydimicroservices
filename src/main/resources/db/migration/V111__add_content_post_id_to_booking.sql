-- Phase 4: Social Commerce — Track content-attributed bookings
-- Migration: V111__add_content_post_id_to_booking.sql

ALTER TABLE bookings.booking
    ADD COLUMN IF NOT EXISTS content_post_id BIGINT
        REFERENCES content.content_posts(id) ON DELETE SET NULL;

COMMENT ON COLUMN bookings.booking.content_post_id IS
    'Content post that originated this booking via social commerce flow (nullable)';
