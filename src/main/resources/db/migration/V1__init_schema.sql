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
    display_order INT DEFAULT 0 NOT NULL,
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
    ('OWNER', 'Property owner with full management rights', 2);


-- ============================================
-- PERFORMANCE OPTIMIZATION COMMENTS
-- ============================================

-- VACUUM and ANALYZE recommendations:
-- Run after bulk inserts: VACUUM ANALYZE users.users;
-- Schedule regular maintenance: VACUUM ANALYZE audit.audit_log;