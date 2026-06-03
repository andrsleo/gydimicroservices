-- ─── V102: Property Calendar & Pricing ───────────────────────────────────────
-- Creates unified calendar, season definitions, and season pricing tables
-- in the properties schema. Migrates data from bookings.property_calendar.
-- ─────────────────────────────────────────────────────────────────────────────


-- ─── 1. properties.property_calendar ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS properties.property_calendar (
    id                    BIGSERIAL PRIMARY KEY,
    property_id           BIGINT NOT NULL,
    calendar_date         DATE NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    custom_price_amount   NUMERIC(15, 2),
    custom_price_currency VARCHAR(10),
    block_source          VARCHAR(50),
    booking_id            BIGINT,
    notes                 VARCHAR(500),
    created_by            BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_property_calendar_date UNIQUE (property_id, calendar_date),
    CONSTRAINT fk_calendar_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE,
    CONSTRAINT chk_calendar_status CHECK (status IN ('AVAILABLE', 'BLOCKED', 'RESERVED', 'MAINTENANCE')),
    CONSTRAINT chk_custom_price_positive CHECK (custom_price_amount IS NULL OR custom_price_amount > 0),
    CONSTRAINT chk_custom_price_currency_pair CHECK (
        (custom_price_amount IS NULL AND custom_price_currency IS NULL) OR
        (custom_price_amount IS NOT NULL AND custom_price_currency IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_prop_calendar_property_date
    ON properties.property_calendar(property_id, calendar_date);

CREATE INDEX IF NOT EXISTS idx_prop_calendar_status
    ON properties.property_calendar(property_id, status)
    WHERE status != 'AVAILABLE';

CREATE INDEX IF NOT EXISTS idx_prop_calendar_booking
    ON properties.property_calendar(booking_id)
    WHERE booking_id IS NOT NULL;


-- ─── 2. properties.season_definition ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS properties.season_definition (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    season_type VARCHAR(20) NOT NULL,
    country     VARCHAR(100) NOT NULL,
    region      VARCHAR(100),
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    recurrence  VARCHAR(20) NOT NULL DEFAULT 'NONE',
    is_system   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_season_type CHECK (season_type IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_season_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_recurrence CHECK (recurrence IN ('NONE', 'YEARLY'))
);

CREATE INDEX IF NOT EXISTS idx_season_country_dates
    ON properties.season_definition(country, start_date, end_date);


-- ─── 3. properties.property_season_pricing ───────────────────────────────────

CREATE TABLE IF NOT EXISTS properties.property_season_pricing (
    id               BIGSERIAL PRIMARY KEY,
    property_id      BIGINT NOT NULL,
    season_type      VARCHAR(20) NOT NULL,
    price_multiplier NUMERIC(4, 2) NOT NULL DEFAULT 1.00,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_season_pricing_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE,
    CONSTRAINT uq_property_season_type UNIQUE (property_id, season_type),
    CONSTRAINT chk_season_pricing_type CHECK (season_type IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_multiplier_range CHECK (price_multiplier BETWEEN 0.10 AND 10.00)
);


-- ─── 4. LATAM season seed data ────────────────────────────────────────────────

-- Colombia
INSERT INTO properties.season_definition (name, season_type, country, region, start_date, end_date, recurrence, is_system)
VALUES
    ('Navidad y Año Nuevo',  'HIGH',   'Colombia', NULL, '2026-12-15', '2027-01-10', 'YEARLY', TRUE),
    ('Semana Santa 2026',    'HIGH',   'Colombia', NULL, '2026-03-29', '2026-04-12', 'NONE',   TRUE),
    ('Mitad de Año',         'MEDIUM', 'Colombia', NULL, '2026-06-15', '2026-07-15', 'YEARLY', TRUE),
    ('Temporada Baja Q1',    'LOW',    'Colombia', NULL, '2026-01-15', '2026-03-15', 'YEARLY', TRUE),
    ('Temporada Baja Q3',    'LOW',    'Colombia', NULL, '2026-08-01', '2026-10-30', 'YEARLY', TRUE);

-- México
INSERT INTO properties.season_definition (name, season_type, country, region, start_date, end_date, recurrence, is_system)
VALUES
    ('Navidad y Año Nuevo',  'HIGH',   'México', NULL, '2026-12-15', '2027-01-10', 'YEARLY', TRUE),
    ('Semana Santa 2026',    'HIGH',   'México', NULL, '2026-03-29', '2026-04-12', 'NONE',   TRUE),
    ('Verano',               'HIGH',   'México', NULL, '2026-07-01', '2026-08-15', 'YEARLY', TRUE),
    ('Día de Muertos',       'MEDIUM', 'México', NULL, '2026-10-28', '2026-11-03', 'YEARLY', TRUE),
    ('Temporada Baja',       'LOW',    'México', NULL, '2026-01-15', '2026-03-15', 'YEARLY', TRUE);


-- ─── 5. Migrate data from bookings.property_calendar ─────────────────────────

INSERT INTO properties.property_calendar (
    property_id,
    calendar_date,
    status,
    block_source,
    booking_id,
    created_by,
    created_at
)
SELECT
    property_id,
    blocked_date                                        AS calendar_date,
    CASE
        WHEN block_reason = 'BOOKING' THEN 'RESERVED'
        ELSE 'BLOCKED'
    END                                                 AS status,
    block_reason                                        AS block_source,
    booking_id,
    created_by_host_id                                  AS created_by,
    created_at
FROM bookings.property_calendar
ON CONFLICT (property_id, calendar_date) DO NOTHING;


-- ─── 6. Deprecate old table ───────────────────────────────────────────────────

COMMENT ON TABLE bookings.property_calendar IS
    '⚠️ DEPRECATED: Migrada a properties.property_calendar en V102. No usar en código nuevo.';
