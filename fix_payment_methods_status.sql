-- ============================================================================
-- FIX: Agregar campo status a payment_methods existentes
-- ============================================================================
-- Este script verifica si el campo status existe, y si no, lo crea y
-- setea todas las tarjetas existentes como ACTIVE
--
-- EJECUTAR ESTE SCRIPT MANUALMENTE EN TU BASE DE DATOS PostgreSQL
-- ============================================================================

-- Verificar si el tipo enum ya existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_status') THEN
        -- Crear el tipo enum
        CREATE TYPE subscriptions.payment_method_status AS ENUM (
            'ACTIVE',
            'INACTIVE',
            'EXPIRED',
            'FAILED',
            'REPLACED'
        );
        RAISE NOTICE 'Enum payment_method_status creado exitosamente';
    ELSE
        RAISE NOTICE 'Enum payment_method_status ya existe';
    END IF;
END $$;

-- Verificar si la columna status ya existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'subscriptions'
        AND table_name = 'payment_methods'
        AND column_name = 'status'
    ) THEN
        -- Agregar la columna status con valor por defecto ACTIVE
        ALTER TABLE subscriptions.payment_methods
        ADD COLUMN status subscriptions.payment_method_status NOT NULL DEFAULT 'ACTIVE';

        RAISE NOTICE 'Columna status agregada exitosamente a payment_methods';

        -- Setear todas las tarjetas existentes como ACTIVE
        UPDATE subscriptions.payment_methods
        SET status = 'ACTIVE'::subscriptions.payment_method_status
        WHERE status IS NULL OR status = 'ACTIVE'::subscriptions.payment_method_status;

        RAISE NOTICE 'Todas las tarjetas existentes marcadas como ACTIVE';
    ELSE
        RAISE NOTICE 'Columna status ya existe en payment_methods';
    END IF;
END $$;

-- Verificar el resultado
SELECT
    COUNT(*) as total_cards,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_cards,
    COUNT(CASE WHEN status = 'INACTIVE' THEN 1 END) as inactive_cards
FROM subscriptions.payment_methods;

-- Mostrar algunas tarjetas de ejemplo
SELECT
    id,
    user_id,
    card_last_four,
    is_active,
    status,
    deleted_at,
    created_at
FROM subscriptions.payment_methods
ORDER BY created_at DESC
LIMIT 5;
