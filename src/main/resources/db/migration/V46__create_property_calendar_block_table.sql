-- Create table for storing blocked dates from external calendars (Airbnb, etc.)
CREATE TABLE properties.property_calendar_blocks (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    source VARCHAR(50) NOT NULL, -- 'AIRBNB', 'MANUAL', etc.
    external_uid VARCHAR(255),   -- UID from the iCal event
    raw_data TEXT,               -- Store raw event data for debugging
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_property_calendar_blocks_property
        FOREIGN KEY (property_id)
        REFERENCES properties.properties (id)
        ON DELETE CASCADE
);

-- Index for faster querying of blocks by property and date range
CREATE INDEX idx_property_calendar_blocks_property_date 
    ON properties.property_calendar_blocks (property_id, start_date, end_date);

-- Create ShedLock table for distributed task scheduling
-- We put this in the public schema or a shared schema, usually public is fine for shedlock
CREATE TABLE public.shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
