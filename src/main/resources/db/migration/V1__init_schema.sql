-- ============================================
-- SCHEMA: users
-- Description: User management and authentication
-- ============================================
CREATE SCHEMA IF NOT EXISTS users;

-- Users table with partitioning support for future scalability
CREATE TABLE users.users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    email_verified BOOLEAN DEFAULT false NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE TABLE users.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT role_name_upper CHECK (name = UPPER(name))
);

CREATE TABLE users.user_roles (
    user_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES users.roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT NOW() NOT NULL,
    assigned_by BIGINT REFERENCES users.users(id),
    PRIMARY KEY (user_id, role_id)
);

-- Indexes for users schema
CREATE UNIQUE INDEX idx_users_email_lower ON users.users(LOWER(email));
CREATE INDEX idx_users_is_active ON users.users(is_active) WHERE is_active = true;
CREATE INDEX idx_users_created_at ON users.users(created_at DESC);
CREATE INDEX idx_user_roles_user_id ON users.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON users.user_roles(role_id);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION users.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users.users
    FOR EACH ROW EXECUTE FUNCTION users.update_updated_at_column();

-- ============================================
-- SCHEMA: properties
-- Description: Property catalog and management
-- ============================================
CREATE SCHEMA IF NOT EXISTS properties;

CREATE TABLE properties.properties (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location JSONB NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL CHECK (price_per_night >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    bedrooms INT CHECK (bedrooms >= 0),
    bathrooms INT CHECK (bathrooms >= 0),
    beds INT CHECK (beds >= 0),
    capacity INT NOT NULL CHECK (capacity > 0),
    principal_image TEXT,
    is_active BOOLEAN DEFAULT true NOT NULL,
    is_featured BOOLEAN DEFAULT false NOT NULL,
    views_count BIGINT DEFAULT 0 NOT NULL,
    rating_avg DECIMAL(3,2) CHECK (rating_avg >= 0 AND rating_avg <= 5),
    reviews_count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT currency_code CHECK (LENGTH(currency) = 3)
);

CREATE TABLE properties.property_owners (
    property_id BIGINT NOT NULL REFERENCES properties.properties(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL, -- FK to users.users
    ownership_percentage DECIMAL(5,2) DEFAULT 100.00 NOT NULL,
    is_primary BOOLEAN DEFAULT false NOT NULL,
    assigned_at TIMESTAMP DEFAULT NOW() NOT NULL,
    PRIMARY KEY (property_id, user_id),
    CONSTRAINT valid_ownership CHECK (ownership_percentage > 0 AND ownership_percentage <= 100)
);

CREATE TABLE properties.amenities_catalog (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    description TEXT,
    is_active BOOLEAN DEFAULT true NOT NULL,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE TABLE properties.property_amenities (
    property_id BIGINT NOT NULL REFERENCES properties.properties(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES properties.amenities_catalog(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT NOW() NOT NULL,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE properties.property_media (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties.properties(id) ON DELETE CASCADE,
    media_url TEXT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100),
    display_order INT DEFAULT 0 NOT NULL,
    is_featured BOOLEAN DEFAULT false NOT NULL,
    width INT,
    height INT,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT valid_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

-- Indexes for properties schema
CREATE INDEX idx_properties_is_active ON properties.properties(is_active) WHERE is_active = true;
CREATE INDEX idx_properties_price ON properties.properties(price_per_night) WHERE is_active = true;
CREATE INDEX idx_properties_capacity ON properties.properties(capacity) WHERE is_active = true;
CREATE INDEX idx_properties_location_gin ON properties.properties USING gin(location);
CREATE INDEX idx_properties_created_at ON properties.properties(created_at DESC);
CREATE INDEX idx_properties_rating ON properties.properties(rating_avg DESC NULLS LAST) WHERE is_active = true;
CREATE INDEX idx_properties_featured ON properties.properties(is_featured, created_at DESC) WHERE is_active = true AND is_featured = true;
CREATE INDEX idx_property_owners_user_id ON properties.property_owners(user_id);
CREATE INDEX idx_property_owners_property_id ON properties.property_owners(property_id);
CREATE INDEX idx_amenities_category ON properties.amenities_catalog(category) WHERE is_active = true;
CREATE INDEX idx_property_amenities_amenity_id ON properties.property_amenities(amenity_id);
CREATE INDEX idx_property_media_property_id ON properties.property_media(property_id);
CREATE INDEX idx_property_media_type ON properties.property_media(media_type);

-- Triggers for properties schema
CREATE TRIGGER update_properties_updated_at BEFORE UPDATE ON properties.properties
    FOR EACH ROW EXECUTE FUNCTION users.update_updated_at_column();

-- ============================================
-- SCHEMA: bookings
-- Description: Booking and reservation management
-- ============================================
CREATE SCHEMA IF NOT EXISTS bookings;

CREATE TABLE bookings.booking_status (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_final BOOLEAN DEFAULT false NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT status_name_upper CHECK (name = UPPER(name))
);

-- Partitioned bookings table for better performance with large datasets
CREATE TABLE bookings.bookings (
    id BIGSERIAL,
    property_id BIGINT NOT NULL, -- FK to properties.properties
    user_id BIGINT NOT NULL, -- FK to users.users (guest)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    nights INT GENERATED ALWAYS AS (end_date - start_date) STORED,
    total_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    price_per_night DECIMAL(10,2) GENERATED ALWAYS AS (total_price / NULLIF(end_date - start_date, 0)) STORED,
    status_id BIGINT NOT NULL REFERENCES bookings.booking_status(id),
    guest_count INT CHECK (guest_count > 0),
    special_requests TEXT,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    PRIMARY KEY (id, start_date),
    CONSTRAINT check_date_range CHECK (end_date > start_date),
    CONSTRAINT check_total_price CHECK (total_price >= 0),
    CONSTRAINT currency_code CHECK (LENGTH(currency) = 3)
) PARTITION BY RANGE (start_date);

-- Create partitions for current and next years
CREATE TABLE bookings.bookings_2024 PARTITION OF bookings.bookings
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE bookings.bookings_2025 PARTITION OF bookings.bookings
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE bookings.bookings_2026 PARTITION OF bookings.bookings
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- Booking availability tracking
CREATE TABLE bookings.property_availability (
    property_id BIGINT NOT NULL, -- FK to properties.properties
    blocked_date DATE NOT NULL,
    reason VARCHAR(50) NOT NULL,
    booking_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    PRIMARY KEY (property_id, blocked_date),
    CONSTRAINT valid_reason CHECK (reason IN ('BOOKING', 'MAINTENANCE', 'OWNER_USE', 'BLOCKED'))
);

-- Indexes for bookings schema
CREATE INDEX idx_bookings_property_id ON bookings.bookings(property_id);
CREATE INDEX idx_bookings_user_id ON bookings.bookings(user_id);
CREATE INDEX idx_bookings_status_id ON bookings.bookings(status_id);
CREATE INDEX idx_bookings_date_range ON bookings.bookings(start_date, end_date);
CREATE INDEX idx_bookings_created_at ON bookings.bookings(created_at DESC);
CREATE INDEX idx_bookings_property_dates ON bookings.bookings(property_id, start_date, end_date);
CREATE INDEX idx_bookings_user_dates ON bookings.bookings(user_id, start_date DESC);
CREATE INDEX idx_property_availability_date ON bookings.property_availability(blocked_date);
CREATE INDEX idx_property_availability_booking ON bookings.property_availability(booking_id) WHERE booking_id IS NOT NULL;

-- Triggers for bookings schema
CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON bookings.bookings
    FOR EACH ROW EXECUTE FUNCTION users.update_updated_at_column();

-- Function to prevent overlapping bookings
CREATE OR REPLACE FUNCTION bookings.check_booking_overlap()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM bookings.bookings
        WHERE property_id = NEW.property_id
        AND id != COALESCE(NEW.id, 0)
        AND status_id IN (
            SELECT id FROM bookings.booking_status
            WHERE name IN ('CONFIRMED', 'IN_PROGRESS', 'PENDING')
        )
        AND (
            (NEW.start_date >= start_date AND NEW.start_date < end_date)
            OR (NEW.end_date > start_date AND NEW.end_date <= end_date)
            OR (NEW.start_date <= start_date AND NEW.end_date >= end_date)
        )
    ) THEN
        RAISE EXCEPTION 'Booking dates overlap with existing booking for this property';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_booking_overlap_trigger
    BEFORE INSERT OR UPDATE ON bookings.bookings
    FOR EACH ROW EXECUTE FUNCTION bookings.check_booking_overlap();

-- ============================================
-- SCHEMA: audit
-- Description: Audit trail and change tracking
-- ============================================
CREATE SCHEMA IF NOT EXISTS audit;

-- Generic audit log table for all schema changes
CREATE TABLE audit.audit_log (
    id BIGSERIAL PRIMARY KEY,
    schema_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    record_id BIGINT NOT NULL,
    old_data JSONB,
    new_data JSONB,
    changed_fields TEXT[],
    user_id BIGINT,
    user_email VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT valid_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE'))
);

-- Indexes for audit schema
CREATE INDEX idx_audit_log_table ON audit.audit_log(schema_name, table_name);
CREATE INDEX idx_audit_log_record_id ON audit.audit_log(record_id);
CREATE INDEX idx_audit_log_operation ON audit.audit_log(operation);
CREATE INDEX idx_audit_log_user_id ON audit.audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit.audit_log(created_at DESC);
CREATE INDEX idx_audit_log_table_record ON audit.audit_log(schema_name, table_name, record_id);

-- Generic audit trigger function
CREATE OR REPLACE FUNCTION audit.log_audit_event()
RETURNS TRIGGER AS $$
DECLARE
    old_data_json JSONB;
    new_data_json JSONB;
    changed_fields TEXT[];
    current_user_id BIGINT;
    current_user_email VARCHAR(255);
BEGIN
    -- Get current user from session variables (set by application)
    BEGIN
        current_user_id := current_setting('app.current_user_id')::BIGINT;
        current_user_email := current_setting('app.current_user_email');
    EXCEPTION
        WHEN OTHERS THEN
            current_user_id := NULL;
            current_user_email := NULL;
    END;

    -- Handle different operations
    IF TG_OP = 'DELETE' THEN
        old_data_json := row_to_json(OLD)::JSONB;
        new_data_json := NULL;

        INSERT INTO audit.audit_log (
            schema_name, table_name, operation, record_id,
            old_data, new_data, changed_fields,
            user_id, user_email
        ) VALUES (
            TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, OLD.id,
            old_data_json, new_data_json, NULL,
            current_user_id, current_user_email
        );

        RETURN OLD;

    ELSIF TG_OP = 'INSERT' THEN
        old_data_json := NULL;
        new_data_json := row_to_json(NEW)::JSONB;

        INSERT INTO audit.audit_log (
            schema_name, table_name, operation, record_id,
            old_data, new_data, changed_fields,
            user_id, user_email
        ) VALUES (
            TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, NEW.id,
            old_data_json, new_data_json, NULL,
            current_user_id, current_user_email
        );

        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        old_data_json := row_to_json(OLD)::JSONB;
        new_data_json := row_to_json(NEW)::JSONB;

        -- Calculate changed fields
        SELECT ARRAY_AGG(key)
        INTO changed_fields
        FROM jsonb_each(old_data_json)
        WHERE old_data_json->key IS DISTINCT FROM new_data_json->key;

        -- Only log if there are actual changes
        IF changed_fields IS NOT NULL AND array_length(changed_fields, 1) > 0 THEN
            INSERT INTO audit.audit_log (
                schema_name, table_name, operation, record_id,
                old_data, new_data, changed_fields,
                user_id, user_email
            ) VALUES (
                TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, NEW.id,
                old_data_json, new_data_json, changed_fields,
                current_user_id, current_user_email
            );
        END IF;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- AUDIT TRIGGERS FOR ALL TABLES
-- ============================================

-- Users schema audit triggers
CREATE TRIGGER audit_users_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users.users
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_roles_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users.roles
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_user_roles_trigger
    AFTER INSERT OR DELETE ON users.user_roles
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

-- Properties schema audit triggers
CREATE TRIGGER audit_properties_trigger
    AFTER INSERT OR UPDATE OR DELETE ON properties.properties
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_owners_trigger
    AFTER INSERT OR UPDATE OR DELETE ON properties.property_owners
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_amenities_trigger
    AFTER INSERT OR UPDATE OR DELETE ON properties.amenities_catalog
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_amenities_trigger
    AFTER INSERT OR DELETE ON properties.property_amenities
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_media_trigger
    AFTER INSERT OR UPDATE OR DELETE ON properties.property_media
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

-- Bookings schema audit triggers
CREATE TRIGGER audit_bookings_trigger
    AFTER INSERT OR UPDATE OR DELETE ON bookings.bookings
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_booking_status_trigger
    AFTER INSERT OR UPDATE OR DELETE ON bookings.booking_status
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

-- ============================================
-- AUDIT HELPER FUNCTIONS
-- ============================================

-- Function to get audit history for a specific record
CREATE OR REPLACE FUNCTION audit.get_record_history(
    p_schema_name VARCHAR,
    p_table_name VARCHAR,
    p_record_id BIGINT
)
RETURNS TABLE (
    id BIGINT,
    operation VARCHAR,
    old_data JSONB,
    new_data JSONB,
    changed_fields TEXT[],
    user_email VARCHAR,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        al.id,
        al.operation,
        al.old_data,
        al.new_data,
        al.changed_fields,
        al.user_email,
        al.created_at
    FROM audit.audit_log al
    WHERE al.schema_name = p_schema_name
        AND al.table_name = p_table_name
        AND al.record_id = p_record_id
    ORDER BY al.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to get recent changes by user
CREATE OR REPLACE FUNCTION audit.get_user_activity(
    p_user_id BIGINT,
    p_limit INT DEFAULT 100
)
RETURNS TABLE (
    id BIGINT,
    schema_name VARCHAR,
    table_name VARCHAR,
    operation VARCHAR,
    record_id BIGINT,
    changed_fields TEXT[],
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        al.id,
        al.schema_name,
        al.table_name,
        al.operation,
        al.record_id,
        al.changed_fields,
        al.created_at
    FROM audit.audit_log al
    WHERE al.user_id = p_user_id
    ORDER BY al.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- INITIAL DATA
-- ============================================

-- Insert default roles
INSERT INTO users.roles (name, description, display_order) VALUES
    ('ADMIN', 'System administrator with full access', 1),
    ('OWNER', 'Property owner with full management rights', 2),
    ('GUEST', 'Guest user who can book properties', 3);

-- Insert default booking statuses
INSERT INTO bookings.booking_status (name, description, is_final, display_order) VALUES
    ('PENDING', 'Booking created but not yet confirmed', false, 1),
    ('CONFIRMED', 'Booking confirmed by owner', false, 2),
    ('IN_PROGRESS', 'Guest is currently staying', false, 3),
    ('COMPLETED', 'Booking completed successfully', true, 4),
    ('CANCELLED', 'Booking cancelled', true, 5);

-- Insert common amenities
INSERT INTO properties.amenities_catalog (name, category, icon, display_order) VALUES
    ('WiFi', 'Internet', 'wifi', 1),
    ('Air Conditioning', 'Climate Control', 'ac_unit', 2),
    ('Heating', 'Climate Control', 'heat', 3),
    ('Kitchen', 'Facilities', 'kitchen', 4),
    ('Washer', 'Appliances', 'local_laundry_service', 5),
    ('Dryer', 'Appliances', 'dry', 6),
    ('TV', 'Entertainment', 'tv', 7),
    ('Pool', 'Outdoor', 'pool', 8),
    ('Parking', 'Outdoor', 'local_parking', 9),
    ('Gym', 'Facilities', 'fitness_center', 10),
    ('Pet Friendly', 'Policies', 'pets', 11),
    ('Smoke Alarm', 'Safety', 'smoke_detector', 12),
    ('First Aid Kit', 'Safety', 'medical_services', 13),
    ('Fire Extinguisher', 'Safety', 'fire_extinguisher', 14);

-- ============================================
-- PERFORMANCE OPTIMIZATION COMMENTS
-- ============================================

-- VACUUM and ANALYZE recommendations:
-- Run after bulk inserts: VACUUM ANALYZE users.users;
-- Run after bulk inserts: VACUUM ANALYZE properties.properties;
-- Run after bulk inserts: VACUUM ANALYZE bookings.bookings;

-- Statistics optimization:
-- ALTER TABLE properties.properties ALTER COLUMN location SET STATISTICS 1000;
-- ALTER TABLE bookings.bookings ALTER COLUMN start_date SET STATISTICS 1000;

