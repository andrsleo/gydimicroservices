-- ============================================================================
-- DIAGNÓSTICO: Verificar estado de payment_methods
-- ============================================================================
-- EJECUTA ESTE SCRIPT COMPLETO EN TU BASE DE DATOS
-- ============================================================================

-- 1. Verificar si la columna 'status' existe
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'subscriptions'
  AND table_name = 'payment_methods'
  AND column_name IN ('status', 'is_active', 'deleted_at')
ORDER BY column_name;

-- 2. Verificar si el tipo enum existe
SELECT
    typname as enum_name,
    enumlabel as enum_value
FROM pg_type
JOIN pg_enum ON pg_type.oid = pg_enum.enumtypid
WHERE typname = 'payment_method_status'
ORDER BY enumsortorder;

-- 3. Contar tarjetas totales y por estado
SELECT
    COUNT(*) as total_cards,
    COUNT(CASE WHEN is_active = true THEN 1 END) as is_active_true,
    COUNT(CASE WHEN is_active = false THEN 1 END) as is_active_false,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as status_active,
    COUNT(CASE WHEN status = 'INACTIVE' THEN 1 END) as status_inactive,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as status_expired,
    COUNT(CASE WHEN status IS NULL THEN 1 END) as status_null
FROM subscriptions.payment_methods;

-- 4. Ver todas las tarjetas con sus estados (máximo 20)
SELECT
    id,
    user_id,
    card_last_four,
    card_brand,
    is_active,
    status,
    deleted_at,
    created_at,
    updated_at
FROM subscriptions.payment_methods
ORDER BY created_at DESC
LIMIT 20;

-- 5. Verificar historial de migraciones Flyway
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
WHERE description LIKE '%payment%' OR description LIKE '%status%'
ORDER BY installed_rank DESC
LIMIT 10;

-- 6. Buscar usuario específico (REEMPLAZA 1 con el ID del usuario que estás probando)
SELECT
    pm.id,
    pm.user_id,
    pm.card_last_four,
    pm.card_brand,
    pm.is_active,
    pm.status,
    pm.is_default,
    pm.deleted_at,
    u.username,
    u.email
FROM subscriptions.payment_methods pm
LEFT JOIN users.users u ON pm.user_id = u.id
WHERE pm.user_id = 1  -- CAMBIAR ESTE ID POR EL USUARIO QUE ESTÁS PROBANDO
ORDER BY pm.is_default DESC, pm.created_at DESC;

-- 7. Verificar si hay alguna constraint bloqueando
SELECT
    conname as constraint_name,
    contype as constraint_type,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conrelid = 'subscriptions.payment_methods'::regclass
ORDER BY conname;
