# Database Migration Checklist - Booking & Payment System

**Project:** GYDI 2.0
**Migration Versions:** V1.5 - V1.8
**Date:** 2025-11-24

---

## Pre-Migration Checklist

### 1. Verify Prerequisites

- [ ] **PostgreSQL 16** is installed and running
- [ ] **Database exists**: `gydi_db` (or configured database name)
- [ ] **Database user** has necessary permissions:
  - [ ] CREATE TABLE
  - [ ] CREATE SCHEMA
  - [ ] CREATE TYPE
  - [ ] CREATE INDEX
  - [ ] CREATE TRIGGER
  - [ ] CREATE FUNCTION

### 2. Backup Database

```bash
# Create backup before running migrations
pg_dump -U postgres -d gydi_db -F c -f backup_before_v1.5_$(date +%Y%m%d_%H%M%S).dump

# Verify backup was created
ls -lh backup_before_v1.5_*.dump
```

### 3. Verify Current Migration State

```bash
cd GydiMicroservices

# Check current migration version
./mvnw flyway:info

# Expected: Last applied version should be < V1.5
```

```sql
-- Or check directly in database
SELECT version, description, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

---

## Migration Files Verification

### 4. Verify Migration Files Exist

```bash
cd GydiMicroservices/src/main/resources/db/migration/

# Check files are present
ls -la V1.5__create_booking_enums.sql
ls -la V1.6__create_bookings_table.sql
ls -la V1.7__create_payment_schema.sql
ls -la V1.8__create_payment_booking_table.sql

# Verify file permissions (should be readable)
# All files should show read permissions (r--)
```

### 5. Verify File Integrity

```bash
# Check files are not empty
wc -l V1.5__create_booking_enums.sql  # Should show ~40 lines
wc -l V1.6__create_bookings_table.sql  # Should show ~150 lines
wc -l V1.7__create_payment_schema.sql  # Should show ~30 lines
wc -l V1.8__create_payment_booking_table.sql  # Should show ~200 lines
```

### 6. Review Migration Scripts

- [ ] **V1.5**: Creates ENUM types (booking_status, payment_status)
- [ ] **V1.6**: Creates referrals.booking table with indexes and triggers
- [ ] **V1.7**: Creates payment schema
- [ ] **V1.8**: Creates payment.booking table with indexes and triggers

**Manual Review (Recommended):**
```bash
# Open each file and verify:
# 1. BEGIN/COMMIT statements are present
# 2. No syntax errors
# 3. All constraints are defined
# 4. Rollback comments are present

cat V1.5__create_booking_enums.sql
cat V1.6__create_bookings_table.sql
cat V1.7__create_payment_schema.sql
cat V1.8__create_payment_booking_table.sql
```

---

## Running Migrations

### 7. Run Migrations (Automatic - Recommended)

```bash
cd GydiMicroservices

# Start Spring Boot application (Flyway runs automatically)
./mvnw spring-boot:run

# Watch console output for Flyway migration logs
# Look for:
#   Flyway Community Edition
#   Migrating schema "public" to version "1.5"
#   Migrating schema "public" to version "1.6"
#   Migrating schema "public" to version "1.7"
#   Migrating schema "public" to version "1.8"
#   Successfully applied 4 migrations
```

**Expected Output:**
```
INFO  o.f.c.i.d.DbMigrate - Migrating schema "public" to version "1.5 - create booking enums"
INFO  o.f.c.i.d.DbMigrate - Migrating schema "public" to version "1.6 - create bookings table"
INFO  o.f.c.i.d.DbMigrate - Migrating schema "public" to version "1.7 - create payment schema"
INFO  o.f.c.i.d.DbMigrate - Migrating schema "public" to version "1.8 - create payment booking table"
INFO  o.f.c.i.d.DbMigrate - Successfully applied 4 migrations to schema "public", now at version v1.8
```

### 8. Run Migrations (Manual - Alternative)

```bash
cd GydiMicroservices

# Check what will be migrated
./mvnw flyway:info

# Apply pending migrations
./mvnw flyway:migrate

# Validate migrations
./mvnw flyway:validate
```

---

## Post-Migration Verification

### 9. Verify Migration Success

```bash
# Check Flyway migration history
./mvnw flyway:info

# Should show:
# | 1.5 | create_booking_enums            | Success |
# | 1.6 | create_bookings_table           | Success |
# | 1.7 | create_payment_schema           | Success |
# | 1.8 | create_payment_booking_table    | Success |
```

### 10. Verify Database Objects (SQL)

Connect to your database:

```bash
psql -U postgres -d gydi_db
```

Run these verification queries:

```sql
-- ============================================================
-- 1. Verify ENUM types were created
-- ============================================================
\dT+ referrals.booking_status
\dT+ payment.payment_status

-- Expected output:
-- referrals.booking_status: REQUEST, RESERVED, FINISHED, CANCELED
-- payment.payment_status: PENDING, PROCESSING, SUCCESS, FAILED, CANCELED


-- ============================================================
-- 2. Verify schemas exist
-- ============================================================
\dn

-- Should show:
-- public, referrals, payment (and possibly others)


-- ============================================================
-- 3. Verify tables were created
-- ============================================================
\dt referrals.*
\dt payment.*

-- Expected output:
-- referrals.booking
-- payment.booking


-- ============================================================
-- 4. Verify table structure (referrals.booking)
-- ============================================================
\d referrals.booking

-- Should show:
-- - booking_id (bigserial, PRIMARY KEY)
-- - referral_link_id (bigint, NOT NULL)
-- - property_id (bigint, NOT NULL)
-- - start_date, end_date (date, NOT NULL)
-- - client_email, client_first_name, client_last_name
-- - total_amount (numeric(10,2), NOT NULL)
-- - status (booking_status, NOT NULL, DEFAULT 'REQUEST')
-- - created_at, updated_at (timestamp with time zone)


-- ============================================================
-- 5. Verify table structure (payment.booking)
-- ============================================================
\d payment.booking

-- Should show:
-- - payment_booking_id (bigserial, PRIMARY KEY)
-- - booking_id (bigint, NOT NULL, UNIQUE)
-- - user_id, property_id (bigint, NOT NULL)
-- - percentage_commission (numeric(5,2), NOT NULL)
-- - commission_amount (numeric(10,2), NOT NULL)
-- - payment_status (payment_status, NOT NULL, DEFAULT 'PENDING')
-- - gateway_response (jsonb)
-- - paid_at, created_at, updated_at (timestamp with time zone)


-- ============================================================
-- 6. Verify foreign key constraints
-- ============================================================
SELECT
    tc.table_schema,
    tc.table_name,
    kcu.column_name,
    ccu.table_schema AS foreign_table_schema,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema IN ('referrals', 'payment')
ORDER BY tc.table_schema, tc.table_name;

-- Expected foreign keys:
-- referrals.booking → referrals.referral_link (referral_link_id)
-- referrals.booking → properties.properties (property_id)
-- payment.booking → referrals.booking (booking_id)
-- payment.booking → users.users (user_id)
-- payment.booking → properties.properties (property_id)


-- ============================================================
-- 7. Verify check constraints
-- ============================================================
SELECT
    pgc.conname AS constraint_name,
    pgc.conrelid::regclass AS table_name,
    pg_get_constraintdef(pgc.oid) AS constraint_definition
FROM pg_constraint pgc
WHERE pgc.contype = 'c'
  AND pgc.connamespace IN (
      SELECT oid FROM pg_namespace WHERE nspname IN ('referrals', 'payment')
  )
ORDER BY table_name, constraint_name;

-- Expected check constraints include:
-- chk_booking_dates (end_date > start_date)
-- chk_booking_start_date_future (start_date >= CURRENT_DATE)
-- chk_booking_total_amount (total_amount > 0)
-- chk_payment_percentage_commission (0 <= percentage <= 100)
-- chk_payment_commission_amount (commission_amount >= 0)


-- ============================================================
-- 8. Verify indexes
-- ============================================================
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname IN ('referrals', 'payment')
ORDER BY tablename, indexname;

-- Expected indexes for referrals.booking:
-- 1. booking_pkey (PRIMARY KEY)
-- 2. idx_booking_referral_link_id
-- 3. idx_booking_property_id
-- 4. idx_booking_status
-- 5. idx_booking_date_range
-- 6. idx_booking_created_at
-- 7. idx_booking_referral_active (partial)
-- 8. idx_booking_property_dates_status (partial)

-- Expected indexes for payment.booking:
-- 1. booking_pkey (PRIMARY KEY)
-- 2. payment_booking_booking_id_key (UNIQUE on booking_id)
-- 3. idx_payment_booking_booking_id
-- 4. idx_payment_booking_user_id
-- 5. idx_payment_booking_property_id
-- 6. idx_payment_booking_status
-- 7. idx_payment_booking_created_at
-- 8. idx_payment_booking_user_success (partial)
-- 9. idx_payment_booking_pending (partial)
-- 10. idx_payment_booking_transaction_id (partial)


-- ============================================================
-- 9. Verify triggers
-- ============================================================
SELECT
    tgname AS trigger_name,
    tgrelid::regclass AS table_name,
    proname AS function_name
FROM pg_trigger
JOIN pg_proc ON tgfoid = pg_proc.oid
WHERE tgrelid IN (
    'referrals.booking'::regclass,
    'payment.booking'::regclass
)
ORDER BY table_name, trigger_name;

-- Expected triggers:
-- referrals.booking:
--   - trg_booking_updated_at → update_booking_updated_at()
-- payment.booking:
--   - trg_payment_booking_updated_at → update_payment_booking_updated_at()
--   - trg_payment_booking_paid_at → set_payment_booking_paid_at()


-- ============================================================
-- 10. Verify functions exist
-- ============================================================
SELECT
    n.nspname AS schema_name,
    p.proname AS function_name,
    pg_get_functiondef(p.oid) AS function_definition
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
WHERE n.nspname IN ('referrals', 'payment')
ORDER BY schema_name, function_name;

-- Expected functions:
-- referrals.update_booking_updated_at()
-- payment.update_payment_booking_updated_at()
-- payment.set_payment_booking_paid_at()
```

---

## Functional Testing

### 11. Test Booking Creation

```sql
-- ============================================================
-- Test 1: Create a booking (should succeed)
-- ============================================================
INSERT INTO referrals.booking (
    referral_link_id,
    property_id,
    start_date,
    end_date,
    client_email,
    client_first_name,
    client_last_name,
    total_amount,
    currency
) VALUES (
    1,                          -- Assuming referral_link_id 1 exists
    1,                          -- Assuming property_id 1 exists
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '14 days',
    'test@example.com',
    'Test',
    'User',
    500.00,
    'USD'
) RETURNING booking_id, status, created_at, updated_at;

-- Expected result:
-- booking_id | status  | created_at              | updated_at
-- -----------|---------|-------------------------|------------------------
-- 1          | REQUEST | 2025-11-24 12:00:00+00  | 2025-11-24 12:00:00+00


-- ============================================================
-- Test 2: Verify check constraint (should fail)
-- ============================================================
-- Try to create booking with end_date <= start_date
INSERT INTO referrals.booking (
    referral_link_id, property_id,
    start_date, end_date,
    client_email, client_first_name, client_last_name,
    total_amount, currency
) VALUES (
    1, 1,
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '5 days',  -- INVALID: end_date < start_date
    'test2@example.com', 'Test2', 'User2',
    500.00, 'USD'
);

-- Expected error:
-- ERROR: new row violates check constraint "chk_booking_dates"


-- ============================================================
-- Test 3: Verify updated_at trigger
-- ============================================================
-- Update booking and verify updated_at changes
UPDATE referrals.booking
SET client_email = 'updated@example.com'
WHERE booking_id = 1
RETURNING booking_id, client_email, created_at, updated_at;

-- Expected result:
-- created_at should be unchanged
-- updated_at should be > created_at


-- ============================================================
-- Test 4: Clean up test data
-- ============================================================
DELETE FROM referrals.booking WHERE booking_id = 1;
```

### 12. Test Payment Creation

```sql
-- ============================================================
-- Test 1: Create a payment (should succeed if booking exists)
-- ============================================================
-- First create a finished booking
INSERT INTO referrals.booking (
    referral_link_id, property_id,
    start_date, end_date,
    client_email, client_first_name, client_last_name,
    total_amount, currency, status
) VALUES (
    1, 1,
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '14 days',
    'test@example.com', 'Test', 'User',
    500.00, 'USD', 'FINISHED'
) RETURNING booking_id;

-- Then create payment
INSERT INTO payment.booking (
    booking_id, user_id, property_id,
    percentage_commission, commission_amount, currency
) VALUES (
    1,                      -- booking_id from above
    1,                      -- Assuming user_id 1 exists
    1,                      -- property_id
    5.00,                   -- 5% commission
    25.00,                  -- 500 * 0.05
    'USD'
) RETURNING payment_booking_id, payment_status, created_at;

-- Expected result:
-- payment_booking_id | payment_status | created_at
-- -------------------|----------------|------------------------
-- 1                  | PENDING        | 2025-11-24 12:00:00+00


-- ============================================================
-- Test 2: Verify UNIQUE constraint (should fail)
-- ============================================================
-- Try to create second payment for same booking
INSERT INTO payment.booking (
    booking_id, user_id, property_id,
    percentage_commission, commission_amount, currency
) VALUES (
    1, 1, 1, 5.00, 25.00, 'USD'
);

-- Expected error:
-- ERROR: duplicate key value violates unique constraint "payment_booking_booking_id_key"


-- ============================================================
-- Test 3: Verify paid_at auto-set trigger
-- ============================================================
UPDATE payment.booking
SET payment_status = 'SUCCESS',
    transaction_id = 'test_txn_123'
WHERE payment_booking_id = 1
RETURNING payment_booking_id, payment_status, paid_at;

-- Expected result:
-- paid_at should be automatically set to CURRENT_TIMESTAMP


-- ============================================================
-- Test 4: Clean up test data
-- ============================================================
DELETE FROM payment.booking WHERE payment_booking_id = 1;
DELETE FROM referrals.booking WHERE booking_id = 1;
```

---

## Performance Testing

### 13. Test Query Performance

```sql
-- ============================================================
-- Test 1: Property availability check
-- ============================================================
EXPLAIN (ANALYZE, BUFFERS) SELECT NOT EXISTS (
    SELECT 1 FROM referrals.booking
    WHERE property_id = 1
      AND status IN ('RESERVED', 'FINISHED')
      AND start_date < CURRENT_DATE + INTERVAL '15 days'
      AND end_date > CURRENT_DATE + INTERVAL '10 days'
) AS is_available;

-- Verify:
-- - Uses idx_booking_property_dates_status index
-- - Execution time < 10ms


-- ============================================================
-- Test 2: User earnings calculation
-- ============================================================
EXPLAIN (ANALYZE, BUFFERS) SELECT COALESCE(SUM(commission_amount), 0)
FROM payment.booking
WHERE user_id = 1
  AND payment_status = 'SUCCESS';

-- Verify:
-- - Uses idx_payment_booking_user_success partial index
-- - Execution time < 20ms
```

---

## Rollback Plan (If Needed)

### 14. Emergency Rollback

**WARNING:** Only use in development or if migrations failed catastrophically.

```sql
-- ============================================================
-- ROLLBACK SCRIPT (Use with extreme caution!)
-- ============================================================
BEGIN;

-- Drop payment.booking
DROP TRIGGER IF EXISTS trg_payment_booking_paid_at ON payment.booking CASCADE;
DROP TRIGGER IF EXISTS trg_payment_booking_updated_at ON payment.booking CASCADE;
DROP FUNCTION IF EXISTS payment.set_payment_booking_paid_at() CASCADE;
DROP FUNCTION IF EXISTS payment.update_payment_booking_updated_at() CASCADE;
DROP TABLE IF EXISTS payment.booking CASCADE;

-- Drop payment schema
DROP SCHEMA IF EXISTS payment CASCADE;

-- Drop referrals.booking
DROP TRIGGER IF EXISTS trg_booking_updated_at ON referrals.booking CASCADE;
DROP FUNCTION IF EXISTS referrals.update_booking_updated_at() CASCADE;
DROP TABLE IF EXISTS referrals.booking CASCADE;

-- Drop ENUMs
DROP TYPE IF EXISTS payment.payment_status CASCADE;
DROP TYPE IF EXISTS referrals.booking_status CASCADE;

-- Remove Flyway history entries
DELETE FROM flyway_schema_history
WHERE version IN ('1.5', '1.6', '1.7', '1.8');

COMMIT;

-- Then restore from backup:
-- pg_restore -U postgres -d gydi_db backup_before_v1.5_YYYYMMDD_HHMMSS.dump
```

---

## Post-Migration Tasks

### 15. Update Application Configuration (if needed)

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 16. Document Migration in Project Log

- [ ] Update project README with new schema version
- [ ] Document any application code changes needed
- [ ] Notify team members of new database schema

### 17. Monitor Application Logs

```bash
# Watch application logs for database errors
tail -f logs/application.log | grep -i "database\|sql\|flyway"
```

---

## Success Criteria

Migration is successful if:

- [x] All 4 migrations applied without errors
- [x] Flyway history shows versions 1.5 - 1.8 as "Success"
- [x] 2 ENUM types created
- [x] 2 tables created (referrals.booking, payment.booking)
- [x] 15 indexes created (7 for bookings, 8 for payments)
- [x] 3 triggers created (1 for bookings, 2 for payments)
- [x] 3 functions created
- [x] All foreign key constraints exist
- [x] All check constraints exist
- [x] Test inserts work correctly
- [x] Constraint violations fail as expected
- [x] Triggers execute correctly
- [x] Query performance is acceptable (< 20ms for common queries)

---

## Troubleshooting

### Issue: Migration Fails with "permission denied"

**Solution:**
```sql
-- Grant necessary permissions
GRANT ALL PRIVILEGES ON SCHEMA referrals TO gydi_app_user;
GRANT ALL PRIVILEGES ON SCHEMA payment TO gydi_app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA referrals TO gydi_app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA payment TO gydi_app_user;
```

### Issue: "relation already exists"

**Solution:**
```sql
-- Check if migration was partially applied
SELECT * FROM flyway_schema_history WHERE version >= '1.5';

-- If needed, manually drop objects and retry
DROP TABLE IF EXISTS referrals.booking CASCADE;
DROP TABLE IF EXISTS payment.booking CASCADE;
```

### Issue: Foreign key constraint violations

**Solution:**
```sql
-- Verify parent tables exist
SELECT * FROM referrals.referral_link LIMIT 1;
SELECT * FROM properties.properties LIMIT 1;
SELECT * FROM users.users LIMIT 1;

-- If missing, run earlier migrations first
```

---

## Contact & Support

**Documentation:**
- Full Schema: `/GydiMicroservices/docs/DATABASE_BOOKING_PAYMENT_SCHEMA.md`
- Quick Reference: `/GydiMicroservices/docs/QUICK_REFERENCE_BOOKING_PAYMENT.md`
- ER Diagram: `/GydiMicroservices/docs/ER_DIAGRAM_BOOKING_PAYMENT.txt`

**Questions?**
- Review documentation first
- Check Flyway logs: `./mvnw flyway:info`
- Check database logs: `tail -f /var/log/postgresql/postgresql-16-main.log`

---

**Migration Checklist Version:** 1.0
**Last Updated:** 2025-11-24
**Status:** Ready for Execution
