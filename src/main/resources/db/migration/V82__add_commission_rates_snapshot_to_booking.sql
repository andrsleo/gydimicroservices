-- Add commission rate snapshot columns to bookings.booking table
-- These columns store the commission rates at the time the booking was confirmed (RESERVED status)
-- This ensures that commission calculations remain consistent even if the user changes plans later.

ALTER TABLE bookings.booking
ADD COLUMN host_commission_rate DECIMAL(5, 4),      -- e.g., 0.1500 (15%)
ADD COLUMN affiliate_commission_rate DECIMAL(5, 4); -- e.g., 0.0500 (5%)

COMMENT ON COLUMN bookings.booking.host_commission_rate IS 'Commission rate for the host at the time of reservation';
COMMENT ON COLUMN bookings.booking.affiliate_commission_rate IS 'Commission rate for the affiliate (if any) at the time of reservation';
