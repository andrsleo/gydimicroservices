-- Migration: Limit videos to maximum 2 per property
-- Description: Removes excess videos from properties that have more than 2 videos,
--              keeping only the first 2 videos ordered by display_order

-- Delete videos beyond the first 2 for each property
DELETE FROM properties.property_videos
WHERE id IN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (PARTITION BY property_id ORDER BY display_order) AS row_num
        FROM properties.property_videos
    ) AS ranked
    WHERE row_num > 2
);

-- Add comment explaining the constraint
COMMENT ON TABLE properties.property_videos IS 'Property videos (maximum 2 per property)';