-- ============================================
-- Migration: Fix audit trigger record_id null issue
-- Description: Ensure record_id defaults to 0 when not available
-- ============================================

-- Drop and recreate the audit function with proper NULL handling
DROP FUNCTION IF EXISTS audit.log_audit_event() CASCADE;

CREATE OR REPLACE FUNCTION audit.log_audit_event()
RETURNS TRIGGER AS $$
DECLARE
    old_data_json JSONB;
    new_data_json JSONB;
    changed_fields TEXT[];
    current_user_id BIGINT;
    current_user_email VARCHAR(255);
    record_id_value BIGINT := 0; -- Default to 0
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

        -- Try to get id from old_data_json, keep 0 if not available
        IF old_data_json ? 'id' AND old_data_json->>'id' IS NOT NULL THEN
            BEGIN
                record_id_value := (old_data_json->>'id')::BIGINT;
            EXCEPTION
                WHEN OTHERS THEN
                    record_id_value := 0;
            END;
        END IF;

        INSERT INTO audit.audit_log (
            schema_name, table_name, operation, record_id,
            old_data, new_data, changed_fields,
            user_id, user_email
        ) VALUES (
            TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, record_id_value,
            old_data_json, new_data_json, NULL,
            current_user_id, current_user_email
        );

        RETURN OLD;

    ELSIF TG_OP = 'INSERT' THEN
        old_data_json := NULL;
        new_data_json := row_to_json(NEW)::JSONB;

        -- Try to get id from new_data_json, keep 0 if not available
        IF new_data_json ? 'id' AND new_data_json->>'id' IS NOT NULL THEN
            BEGIN
                record_id_value := (new_data_json->>'id')::BIGINT;
            EXCEPTION
                WHEN OTHERS THEN
                    record_id_value := 0;
            END;
        END IF;

        INSERT INTO audit.audit_log (
            schema_name, table_name, operation, record_id,
            old_data, new_data, changed_fields,
            user_id, user_email
        ) VALUES (
            TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, record_id_value,
            old_data_json, new_data_json, NULL,
            current_user_id, current_user_email
        );

        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        old_data_json := row_to_json(OLD)::JSONB;
        new_data_json := row_to_json(NEW)::JSONB;

        -- Identify changed fields
        SELECT array_agg(key)
        INTO changed_fields
        FROM jsonb_each(old_data_json) old_field
        WHERE old_field.value IS DISTINCT FROM new_data_json->old_field.key;

        -- Try to get id from new_data_json, keep 0 if not available
        IF new_data_json ? 'id' AND new_data_json->>'id' IS NOT NULL THEN
            BEGIN
                record_id_value := (new_data_json->>'id')::BIGINT;
            EXCEPTION
                WHEN OTHERS THEN
                    record_id_value := 0;
            END;
        END IF;

        INSERT INTO audit.audit_log (
            schema_name, table_name, operation, record_id,
            old_data, new_data, changed_fields,
            user_id, user_email
        ) VALUES (
            TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, record_id_value,
            old_data_json, new_data_json, changed_fields,
            current_user_id, current_user_email
        );

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Recreate all audit triggers (matching original trigger names from V1)
CREATE TRIGGER audit_users_trigger AFTER INSERT OR UPDATE OR DELETE ON users.users
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_user_roles_trigger AFTER INSERT OR DELETE ON users.user_roles
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_roles_trigger AFTER INSERT OR UPDATE OR DELETE ON users.roles
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_properties_trigger AFTER INSERT OR UPDATE OR DELETE ON properties.properties
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_amenities_trigger AFTER INSERT OR UPDATE OR DELETE ON properties.amenities_catalog
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_amenities_trigger AFTER INSERT OR DELETE ON properties.property_amenities
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_owners_trigger AFTER INSERT OR UPDATE OR DELETE ON properties.property_owners
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_property_media_trigger AFTER INSERT OR UPDATE OR DELETE ON properties.property_media
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_bookings_trigger AFTER INSERT OR UPDATE OR DELETE ON bookings.bookings
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();

CREATE TRIGGER audit_booking_status_trigger AFTER INSERT OR UPDATE OR DELETE ON bookings.booking_status
    FOR EACH ROW EXECUTE FUNCTION audit.log_audit_event();