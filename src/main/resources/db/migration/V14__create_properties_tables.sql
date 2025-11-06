-- ========================================
-- Properties Tables Migration
-- Version: V14
-- Description: Creates properties schema and tables for property_images and property_videos
-- ========================================

-- Create properties schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS properties;

-- Create properties table
CREATE TABLE IF NOT EXISTS properties.properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    price_amount DECIMAL(10, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    postal_code VARCHAR(20),
    amenities JSONB DEFAULT '[]'::jsonb,
    bedrooms INTEGER NOT NULL DEFAULT 0,
    bathrooms INTEGER NOT NULL DEFAULT 0,
    max_guests INTEGER NOT NULL DEFAULT 1,
    property_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_title_length CHECK (LENGTH(title) >= 10 AND LENGTH(title) <= 100),
    CONSTRAINT chk_description_length CHECK (description IS NULL OR LENGTH(description) <= 2000),
    CONSTRAINT chk_price_positive CHECK (price_amount > 0),
    CONSTRAINT chk_bedrooms_non_negative CHECK (bedrooms >= 0),
    CONSTRAINT chk_bathrooms_non_negative CHECK (bathrooms >= 0),
    CONSTRAINT chk_max_guests_positive CHECK (max_guests >= 1),
    CONSTRAINT chk_property_type CHECK (property_type IN ('APARTMENT', 'HOUSE', 'VILLA', 'CABIN', 'STUDIO', 'CONDO', 'BUNGALOW', 'OTHER')),
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_currency CHECK (price_currency IN ('USD', 'EUR', 'MXN', 'CAD', 'GBP'))
);

-- Create property_images table
CREATE TABLE IF NOT EXISTS properties.property_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_url_not_empty CHECK (LENGTH(url) > 0),
    CONSTRAINT chk_display_order_non_negative CHECK (display_order >= 0),
    CONSTRAINT uq_property_image_order UNIQUE (property_id, display_order)
);

-- Create property_videos table
CREATE TABLE IF NOT EXISTS properties.property_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    duration_seconds INTEGER,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_property_videos_property FOREIGN KEY (property_id)
        REFERENCES properties.properties(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_video_url_not_empty CHECK (LENGTH(url) > 0),
    CONSTRAINT chk_video_display_order_non_negative CHECK (display_order >= 0),
    CONSTRAINT chk_duration_non_negative CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT uq_property_video_order UNIQUE (property_id, display_order)
);

-- Create indexes for performance
CREATE INDEX idx_properties_host_id ON properties.properties(host_id);
CREATE INDEX idx_properties_status ON properties.properties(status);
CREATE INDEX idx_properties_property_type ON properties.properties(property_type);
CREATE INDEX idx_properties_location ON properties.properties(country, city);
CREATE INDEX idx_properties_price ON properties.properties(price_amount);
CREATE INDEX idx_properties_specs ON properties.properties(bedrooms, bathrooms, max_guests);
CREATE INDEX idx_properties_created_at ON properties.properties(created_at DESC);
CREATE INDEX idx_properties_published_at ON properties.properties(published_at DESC) WHERE published_at IS NOT NULL;

-- Full-text search index on title and description
CREATE INDEX idx_properties_search ON properties.properties USING GIN (to_tsvector('english', title || ' ' || COALESCE(description, '')));

-- Amenities GIN index for JSONB queries
CREATE INDEX idx_properties_amenities ON properties.properties USING GIN (amenities);

-- Indexes for property_images
CREATE INDEX idx_property_images_property_id ON properties.property_images(property_id);
CREATE INDEX idx_property_images_display_order ON properties.property_images(property_id, display_order);

-- Indexes for property_videos
CREATE INDEX idx_property_videos_property_id ON properties.property_videos(property_id);
CREATE INDEX idx_property_videos_display_order ON properties.property_videos(property_id, display_order);

-- Add comments for documentation
COMMENT ON TABLE properties.properties IS 'Vacation rental properties';
COMMENT ON COLUMN properties.properties.host_id IS 'User ID of the property host/owner';
COMMENT ON COLUMN properties.properties.amenities IS 'JSON array of amenity strings';
COMMENT ON COLUMN properties.properties.status IS 'Property status: DRAFT, PUBLISHED, INACTIVE, DELETED';
COMMENT ON COLUMN properties.properties.deleted IS 'Soft delete flag';

COMMENT ON TABLE properties.property_images IS 'Property images (max 10 per property)';
COMMENT ON COLUMN properties.property_images.display_order IS 'Order in which to display images (0-based)';

COMMENT ON TABLE properties.property_videos IS 'Property videos (max 5 per property)';
COMMENT ON COLUMN properties.property_videos.display_order IS 'Order in which to display videos (0-based)';
COMMENT ON COLUMN properties.property_videos.duration_seconds IS 'Video duration in seconds';

-- Function to enforce max image count
CREATE OR REPLACE FUNCTION properties.check_max_property_images()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM properties.property_images WHERE property_id = NEW.property_id) >= 10 THEN
        RAISE EXCEPTION 'Property cannot have more than 10 images';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_max_property_images
BEFORE INSERT ON properties.property_images
FOR EACH ROW
EXECUTE FUNCTION properties.check_max_property_images();

-- Function to enforce max video count
CREATE OR REPLACE FUNCTION properties.check_max_property_videos()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM properties.property_videos WHERE property_id = NEW.property_id) >= 5 THEN
        RAISE EXCEPTION 'Property cannot have more than 5 videos';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_max_property_videos
BEFORE INSERT ON properties.property_videos
FOR EACH ROW
EXECUTE FUNCTION properties.check_max_property_videos();

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION properties.update_properties_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_properties_updated_at
BEFORE UPDATE ON properties.properties
FOR EACH ROW
EXECUTE FUNCTION properties.update_properties_updated_at();
