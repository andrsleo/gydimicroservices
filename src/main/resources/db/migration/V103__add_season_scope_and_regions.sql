-- ─── V103: Season scope hierarchy + regions ───────────────────────────────────
-- Adds geographic scope (GLOBAL / REGION / COUNTRY / SUBREGION) so a single
-- season definition can cover multiple countries without duplicating rows.
-- Also creates season_region and season_region_country tables.

-- 1. Make country nullable (needed for GLOBAL and REGION scope)
ALTER TABLE properties.season_definition
    ALTER COLUMN country DROP NOT NULL;

-- 2. Add scope column (default COUNTRY keeps backward-compatibility)
ALTER TABLE properties.season_definition
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'COUNTRY';

ALTER TABLE properties.season_definition
    ADD CONSTRAINT chk_season_scope
    CHECK (scope IN ('GLOBAL', 'REGION', 'COUNTRY', 'SUBREGION'));

-- 3. Add region_code column (used when scope = REGION)
ALTER TABLE properties.season_definition
    ADD COLUMN region_code VARCHAR(50);

-- 4. Existing records are all COUNTRY scope — no data change needed

-- ─── 5. Season regions catalog ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS properties.season_region (
    code  VARCHAR(50)  PRIMARY KEY,
    name  VARCHAR(100) NOT NULL
);

INSERT INTO properties.season_region (code, name) VALUES
    ('LATAM',        'América Latina'),
    ('SUDAMERICA',   'Sudamérica'),
    ('CONO_SUR',     'Cono Sur'),
    ('ANDINA',       'Región Andina'),
    ('CARIBE',       'El Caribe'),
    ('CENTROAMERICA','América Central'),
    ('NORTEAMERICA', 'América del Norte'),
    ('EUROPA',       'Europa'),
    ('EUROPA_SUR',   'Europa del Sur'),
    ('GLOBAL',       'Global (todos los países)');

-- ─── 6. Region ↔ country mapping ──────────────────────────────────────────────
-- country matches the string stored in properties.season_definition.country
-- and properties.property_details.country (full country names in Spanish)
CREATE TABLE IF NOT EXISTS properties.season_region_country (
    region_code  VARCHAR(50)  NOT NULL REFERENCES properties.season_region(code),
    country      VARCHAR(100) NOT NULL,
    PRIMARY KEY (region_code, country)
);

INSERT INTO properties.season_region_country (region_code, country) VALUES
    -- LATAM: todo el continente hispanohablante
    ('LATAM', 'Colombia'),
    ('LATAM', 'México'),
    ('LATAM', 'Argentina'),
    ('LATAM', 'Chile'),
    ('LATAM', 'Perú'),
    ('LATAM', 'Venezuela'),
    ('LATAM', 'Ecuador'),
    ('LATAM', 'Bolivia'),
    ('LATAM', 'Paraguay'),
    ('LATAM', 'Uruguay'),
    ('LATAM', 'Costa Rica'),
    ('LATAM', 'Panamá'),
    ('LATAM', 'Guatemala'),
    ('LATAM', 'Honduras'),
    ('LATAM', 'El Salvador'),
    ('LATAM', 'Nicaragua'),
    ('LATAM', 'República Dominicana'),
    ('LATAM', 'Cuba'),
    ('LATAM', 'Puerto Rico'),
    -- SUDAMERICA
    ('SUDAMERICA', 'Colombia'),
    ('SUDAMERICA', 'Argentina'),
    ('SUDAMERICA', 'Chile'),
    ('SUDAMERICA', 'Perú'),
    ('SUDAMERICA', 'Venezuela'),
    ('SUDAMERICA', 'Ecuador'),
    ('SUDAMERICA', 'Bolivia'),
    ('SUDAMERICA', 'Paraguay'),
    ('SUDAMERICA', 'Uruguay'),
    -- CONO_SUR
    ('CONO_SUR', 'Argentina'),
    ('CONO_SUR', 'Chile'),
    ('CONO_SUR', 'Uruguay'),
    ('CONO_SUR', 'Paraguay'),
    -- ANDINA
    ('ANDINA', 'Colombia'),
    ('ANDINA', 'Perú'),
    ('ANDINA', 'Ecuador'),
    ('ANDINA', 'Bolivia'),
    ('ANDINA', 'Venezuela'),
    -- CARIBE
    ('CARIBE', 'Cuba'),
    ('CARIBE', 'República Dominicana'),
    ('CARIBE', 'Puerto Rico'),
    ('CARIBE', 'Jamaica'),
    -- CENTROAMERICA
    ('CENTROAMERICA', 'Costa Rica'),
    ('CENTROAMERICA', 'Panamá'),
    ('CENTROAMERICA', 'Guatemala'),
    ('CENTROAMERICA', 'Honduras'),
    ('CENTROAMERICA', 'El Salvador'),
    ('CENTROAMERICA', 'Nicaragua'),
    -- NORTEAMERICA
    ('NORTEAMERICA', 'México'),
    ('NORTEAMERICA', 'Estados Unidos'),
    ('NORTEAMERICA', 'Canadá'),
    -- EUROPA
    ('EUROPA', 'España'),
    ('EUROPA', 'Francia'),
    ('EUROPA', 'Alemania'),
    ('EUROPA', 'Italia'),
    ('EUROPA', 'Portugal'),
    ('EUROPA', 'Reino Unido'),
    ('EUROPA', 'Países Bajos'),
    ('EUROPA', 'Bélgica'),
    ('EUROPA', 'Suiza'),
    ('EUROPA', 'Austria'),
    -- EUROPA_SUR
    ('EUROPA_SUR', 'España'),
    ('EUROPA_SUR', 'Italia'),
    ('EUROPA_SUR', 'Portugal'),
    ('EUROPA_SUR', 'Grecia');

-- ─── 7. Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_season_scope ON properties.season_definition(scope);
CREATE INDEX IF NOT EXISTS idx_season_region_code ON properties.season_definition(region_code);
CREATE INDEX IF NOT EXISTS idx_src_country ON properties.season_region_country(country);
