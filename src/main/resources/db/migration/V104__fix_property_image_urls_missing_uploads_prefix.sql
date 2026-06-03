-- V104: Fix property image/video URLs missing the /uploads/ path segment
--
-- Root cause: LocalFileSystemStorageAdapter was configured without /uploads in
-- base-url at some point, storing URLs as http://host/properties/{id}/images/{uuid}
-- instead of http://host/uploads/properties/{id}/images/{uuid}.
-- Spring MVC's /uploads/** resource handler cannot serve the old-format URLs.
--
-- Fix: Rewrite every URL of the form http(s)://host/properties/... or
--      http(s)://host/profile-images/... to include /uploads/ before the path.

-- Fix property images
UPDATE properties.property_images
SET url = regexp_replace(url, '^(https?://[^/]+)/properties/', '\1/uploads/properties/')
WHERE url ~ '^https?://[^/]+/properties/';

-- Fix property videos (same issue may affect them)
UPDATE properties.property_videos
SET url = regexp_replace(url, '^(https?://[^/]+)/properties/', '\1/uploads/properties/')
WHERE url ~ '^https?://[^/]+/properties/';

-- Fix thumbnail URLs on videos
UPDATE properties.property_videos
SET thumbnail_url = regexp_replace(thumbnail_url, '^(https?://[^/]+)/properties/', '\1/uploads/properties/')
WHERE thumbnail_url IS NOT NULL
  AND thumbnail_url ~ '^https?://[^/]+/properties/';
