-- Migration: Update maximum images to 20 and videos to 2
-- Description: Updates database triggers to enforce new limits:
--              - Images: 20 (was 10)
--              - Videos: 2 (was 5)

-- Update function to enforce max 20 images per property
CREATE OR REPLACE FUNCTION properties.check_max_property_images()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM properties.property_images WHERE property_id = NEW.property_id) >= 20 THEN
        RAISE EXCEPTION 'Property cannot have more than 20 images';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Update function to enforce max 2 videos per property
CREATE OR REPLACE FUNCTION properties.check_max_property_videos()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM properties.property_videos WHERE property_id = NEW.property_id) >= 2 THEN
        RAISE EXCEPTION 'Property cannot have more than 2 videos';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Update table comments
COMMENT ON TABLE properties.property_images IS 'Property images (maximum 20 per property)';
COMMENT ON TABLE properties.property_videos IS 'Property videos (maximum 2 per property)';