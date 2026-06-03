-- ============================================================
-- V99: Property Calendar + Booking Direct Confirmation Support
-- ============================================================

-- 1. Tabla para gestión de disponibilidad por propiedad
CREATE TABLE IF NOT EXISTS bookings.property_calendar (
    id                   BIGSERIAL PRIMARY KEY,
    property_id          BIGINT NOT NULL,
    blocked_date         DATE NOT NULL,
    block_reason         VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    booking_id           BIGINT,
    created_by_host_id   BIGINT NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_property_calendar_date UNIQUE (property_id, blocked_date),
    CONSTRAINT chk_block_reason CHECK (block_reason IN ('MANUAL', 'BOOKING', 'MAINTENANCE')),
    CONSTRAINT fk_calendar_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE
);

CREATE INDEX idx_property_calendar_property_date
    ON bookings.property_calendar(property_id, blocked_date);

-- 2. Columnas de confirmación directa y T&C en bookings
ALTER TABLE bookings.booking
    ADD COLUMN IF NOT EXISTS terms_accepted_at    TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS terms_accepted_ip    VARCHAR(45),
    ADD COLUMN IF NOT EXISTS confirmed_by_host_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_by_host_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejection_reason     VARCHAR(500);

-- 3. Columnas Stripe para Fase 2 (crear ahora, poblar en Fase 2)
ALTER TABLE bookings.booking
    ADD COLUMN IF NOT EXISTS stripe_booking_intent_id  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_deposit_intent_id  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS deposit_amount            NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS deposit_currency          VARCHAR(3),
    ADD COLUMN IF NOT EXISTS deposit_captured_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deposit_capture_amount    NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS payment_released_at       TIMESTAMP WITH TIME ZONE;

COMMENT ON TABLE bookings.property_calendar IS
    'Host-managed availability calendar. Dates listed here are blocked for new bookings.';
COMMENT ON COLUMN bookings.booking.terms_accepted_at IS
    'Timestamp when guest accepted T&C (host-guest contract, GYDI is not party to it).';
