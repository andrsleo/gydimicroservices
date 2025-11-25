# Booking & Payment Schema Documentation

**Project:** GYDI 2.0
**Database:** PostgreSQL 16
**Migration Tool:** Flyway
**Date:** 2025-11-24
**Version:** 1.5 - 1.8

---

## Table of Contents

1. [Overview](#overview)
2. [Schema Diagrams](#schema-diagrams)
3. [Table Definitions](#table-definitions)
4. [Business Rules](#business-rules)
5. [Index Strategy](#index-strategy)
6. [Sample Queries](#sample-queries)
7. [Migration Execution](#migration-execution)
8. [Common Operations](#common-operations)
9. [Performance Considerations](#performance-considerations)
10. [Future Extensibility](#future-extensibility)

---

## Overview

The booking and payment system consists of two main tables across two schemas:

- **`referrals.booking`**: Tracks booking lifecycle from request to completion/cancellation
- **`payment.booking`**: Records commission payments to referring users for completed bookings

### Design Principles

1. **Separation of Concerns**: Booking domain (`referrals`) separate from payment domain (`payment`)
2. **Data Integrity**: Comprehensive constraints ensure business rules at database level
3. **Audit Trail**: Full timestamp tracking with automatic `updated_at` triggers
4. **Performance**: Strategic indexes for common query patterns
5. **Extensibility**: JSONB fields and nullable columns for future gateway integrations

---

## Schema Diagrams

### Conceptual Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BOOKING LIFECYCLE                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. User generates referral link                                   │
│  2. Client clicks link → views property                            │
│  3. Client submits booking request (status: REQUEST)               │
│  4. Property owner reviews → confirms (status: RESERVED)           │
│  5. Client completes stay → booking finalized (status: FINISHED)   │
│  6. Payment record created → commission calculated                 │
│  7. Payment processed → referring user receives commission         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
users.users
    │
    │ (referring user)
    ▼
referrals.referral_link ─────┐
    │                        │
    │ (generates)            │ (links to)
    ▼                        ▼
referrals.booking ────────► properties.properties
    │
    │ (when status = FINISHED)
    ▼
payment.booking
    │
    │ (pays commission to)
    ▼
users.users (referring user)
```

### Table Relationships

```sql
-- referrals.booking
referral_link_id ──► referrals.referral_link.id (ON DELETE RESTRICT)
property_id ──────► properties.properties.id (ON DELETE RESTRICT)

-- payment.booking
booking_id ───────► referrals.booking.booking_id (ON DELETE RESTRICT, UNIQUE)
user_id ──────────► users.users.id (ON DELETE RESTRICT)
property_id ──────► properties.properties.id (ON DELETE RESTRICT)
```

---

## Table Definitions

### 1. referrals.booking

**Purpose**: Store all booking requests and their lifecycle status.

| Column               | Type                          | Constraints                  | Description                                    |
|----------------------|-------------------------------|------------------------------|------------------------------------------------|
| `booking_id`         | BIGSERIAL                     | PRIMARY KEY                  | Auto-incrementing unique identifier            |
| `referral_link_id`   | BIGINT                        | NOT NULL, FK                 | Referral link that generated this booking      |
| `property_id`        | BIGINT                        | NOT NULL, FK                 | Property being booked (denormalized)           |
| `start_date`         | DATE                          | NOT NULL, ≥ CURRENT_DATE     | Booking start date                             |
| `end_date`           | DATE                          | NOT NULL, > start_date       | Booking end date                               |
| `client_email`       | VARCHAR(255)                  | NOT NULL                     | Client contact email                           |
| `client_first_name`  | VARCHAR(100)                  | NOT NULL                     | Client first name                              |
| `client_last_name`   | VARCHAR(100)                  | NOT NULL                     | Client last name                               |
| `client_phone`       | VARCHAR(20)                   | NULL                         | Client phone number (optional)                 |
| `total_amount`       | DECIMAL(10, 2)                | NOT NULL, > 0                | Total booking cost                             |
| `currency`           | VARCHAR(3)                    | NOT NULL, DEFAULT 'USD'      | Currency code (USD, EUR, GBP, MXN, COP)        |
| `status`             | referrals.booking_status      | NOT NULL, DEFAULT 'REQUEST'  | Booking status (ENUM)                          |
| `cancellation_reason`| TEXT                          | NULL                         | Reason for cancellation                        |
| `canceled_by`        | VARCHAR(50)                   | NULL                         | Who canceled (CLIENT, OWNER, ADMIN, SYSTEM)    |
| `canceled_at`        | TIMESTAMP WITH TIME ZONE      | NULL                         | Cancellation timestamp                         |
| `created_at`         | TIMESTAMP WITH TIME ZONE      | NOT NULL, DEFAULT NOW()      | Record creation timestamp                      |
| `updated_at`         | TIMESTAMP WITH TIME ZONE      | NOT NULL, DEFAULT NOW()      | Last update timestamp (auto-updated)           |

**ENUM: referrals.booking_status**
- `REQUEST` - Initial booking request
- `RESERVED` - Confirmed by property owner
- `FINISHED` - Booking completed (client checked out)
- `CANCELED` - Booking canceled

---

### 2. payment.booking

**Purpose**: Track commission payments to referring users for completed bookings.

| Column                  | Type                          | Constraints                  | Description                                    |
|-------------------------|-------------------------------|------------------------------|------------------------------------------------|
| `payment_booking_id`    | BIGSERIAL                     | PRIMARY KEY                  | Auto-incrementing unique identifier            |
| `booking_id`            | BIGINT                        | NOT NULL, UNIQUE, FK         | 1:1 relationship with referrals.booking        |
| `user_id`               | BIGINT                        | NOT NULL, FK                 | Referring user who earns commission            |
| `property_id`           | BIGINT                        | NOT NULL, FK                 | Property (denormalized)                        |
| `percentage_commission` | DECIMAL(5, 2)                 | NOT NULL, 0-100              | Commission rate (e.g., 5.00 = 5%)              |
| `commission_amount`     | DECIMAL(10, 2)                | NOT NULL, ≥ 0                | Calculated commission amount                   |
| `currency`              | VARCHAR(3)                    | NOT NULL, DEFAULT 'USD'      | Currency code                                  |
| `payment_status`        | payment.payment_status        | NOT NULL, DEFAULT 'PENDING'  | Payment status (ENUM)                          |
| `payment_method`        | VARCHAR(50)                   | NULL                         | Payment method (STRIPE, PAYPAL, etc.)          |
| `transaction_id`        | VARCHAR(255)                  | NULL                         | Gateway transaction ID                         |
| `gateway_name`          | VARCHAR(100)                  | NULL                         | Payment gateway name                           |
| `gateway_response`      | JSONB                         | NULL                         | Full gateway response (for debugging)          |
| `paid_at`               | TIMESTAMP WITH TIME ZONE      | NULL                         | Payment completion timestamp (auto-set)        |
| `created_at`            | TIMESTAMP WITH TIME ZONE      | NOT NULL, DEFAULT NOW()      | Record creation timestamp                      |
| `updated_at`            | TIMESTAMP WITH TIME ZONE      | NOT NULL, DEFAULT NOW()      | Last update timestamp (auto-updated)           |

**ENUM: payment.payment_status**
- `PENDING` - Payment record created, awaiting processing
- `PROCESSING` - Payment being processed by gateway
- `SUCCESS` - Payment completed successfully
- `FAILED` - Payment processing failed
- `CANCELED` - Payment canceled (booking canceled before payment)

---

## Business Rules

### Booking Rules

1. **Date Validation**
   - `start_date` must be >= current date (no past bookings)
   - `end_date` must be > `start_date` (minimum 1 night)

2. **Status Transitions**
   - `REQUEST` → `RESERVED` (owner confirms)
   - `REQUEST` → `CANCELED` (client/owner cancels)
   - `RESERVED` → `FINISHED` (client checks out)
   - `RESERVED` → `CANCELED` (client/owner cancels)
   - ⚠️ No transitions FROM `FINISHED` or `CANCELED` (final states)

3. **Cancellation Requirements**
   - When `status = 'CANCELED'`, must have:
     - `cancellation_reason` (TEXT, required)
     - `canceled_by` (who initiated)
     - `canceled_at` (timestamp)

4. **Amount Validation**
   - `total_amount` must be > 0
   - `currency` must be one of: USD, EUR, GBP, MXN, COP

### Payment Rules

1. **Payment Creation**
   - Payment record created ONLY when `booking.status = 'FINISHED'`
   - Creates 1:1 relationship via UNIQUE constraint on `booking_id`

2. **Commission Calculation**
   ```
   commission_amount = booking.total_amount × (percentage_commission / 100)
   ```
   - `percentage_commission` comes from user's subscription plan at time of booking completion
   - Must be stored (not calculated dynamically) to preserve historical accuracy

3. **Payment Status Transitions**
   - `PENDING` → `PROCESSING` (payment gateway invoked)
   - `PROCESSING` → `SUCCESS` (payment completed)
   - `PROCESSING` → `FAILED` (payment failed)
   - Any state → `CANCELED` (booking canceled before payment)

4. **Success Requirements**
   - When `payment_status = 'SUCCESS'`, must have:
     - `paid_at` (auto-set by trigger)
     - `transaction_id` (gateway transaction ID)

5. **Percentage Validation**
   - `percentage_commission` must be between 0 and 100

---

## Index Strategy

### referrals.booking Indexes

| Index Name                        | Columns                                  | Type       | Purpose                                    |
|-----------------------------------|------------------------------------------|------------|--------------------------------------------|
| `idx_booking_referral_link_id`    | `referral_link_id`                       | B-tree     | JOIN performance with referral_link        |
| `idx_booking_property_id`         | `property_id`                            | B-tree     | JOIN performance with properties           |
| `idx_booking_status`              | `status`                                 | B-tree     | Filter bookings by status                  |
| `idx_booking_date_range`          | `start_date, end_date`                   | B-tree     | Find bookings in date range                |
| `idx_booking_created_at`          | `created_at DESC`                        | B-tree     | Find recent bookings (dashboards)          |
| `idx_booking_referral_active`     | `referral_link_id, status` (partial)     | B-tree     | Find active bookings for a referral link   |
| `idx_booking_property_dates_status` | `property_id, start_date, end_date, status` (partial) | B-tree | Property availability checks |

**Rationale:**
- **Foreign key indexes**: Improve JOIN performance (PostgreSQL doesn't auto-index FKs)
- **Status index**: Common filter in queries ("find all RESERVED bookings")
- **Date range index**: Supports queries like "bookings between Jan 1 - Jan 31"
- **Partial indexes**: Use `WHERE` clauses to reduce index size for common filtered queries
- **Composite index for availability**: Efficient overlap detection for double-booking prevention

### payment.booking Indexes

| Index Name                           | Columns                                   | Type       | Purpose                                    |
|--------------------------------------|-------------------------------------------|------------|--------------------------------------------|
| `idx_payment_booking_booking_id`     | `booking_id`                              | B-tree     | JOIN performance with referrals.booking    |
| `idx_payment_booking_user_id`        | `user_id`                                 | B-tree     | JOIN performance with users                |
| `idx_payment_booking_property_id`    | `property_id`                             | B-tree     | JOIN performance with properties           |
| `idx_payment_booking_status`         | `payment_status`                          | B-tree     | Filter payments by status                  |
| `idx_payment_booking_created_at`     | `created_at DESC`                         | B-tree     | Find recent payments                       |
| `idx_payment_booking_user_success`   | `user_id, payment_status, paid_at DESC` (partial) | B-tree | User earnings history |
| `idx_payment_booking_pending`        | `payment_status, created_at` (partial)    | B-tree     | Batch processing of pending payments       |
| `idx_payment_booking_transaction_id` | `transaction_id` (partial)                | B-tree     | Idempotency checks with payment gateway    |

**Rationale:**
- **User earnings index**: Partial index on SUCCESS payments for fast earnings history queries
- **Pending payments index**: Supports batch processing jobs that need to process PENDING/PROCESSING payments
- **Transaction ID index**: Enables fast lookup for idempotency (prevent duplicate payments)

---

## Sample Queries

### 1. Find All Active Bookings for a Referral Link

```sql
-- Use case: Display bookings generated by a specific referral link
SELECT
    b.booking_id,
    b.start_date,
    b.end_date,
    b.client_email,
    b.client_first_name || ' ' || b.client_last_name AS client_name,
    b.total_amount,
    b.currency,
    b.status,
    p.title AS property_title
FROM referrals.booking b
INNER JOIN properties.properties p ON b.property_id = p.id
WHERE b.referral_link_id = ? -- Parameter: referral link ID
  AND b.status IN ('REQUEST', 'RESERVED')
ORDER BY b.created_at DESC;

-- Performance: Uses idx_booking_referral_active partial index
```

### 2. Check Property Availability (Prevent Double Booking)

```sql
-- Use case: Verify if property is available for requested dates
-- Returns true if available, false if overlapping bookings exist

SELECT NOT EXISTS (
    SELECT 1
    FROM referrals.booking
    WHERE property_id = ? -- Parameter: property ID
      AND status IN ('RESERVED', 'FINISHED')
      AND start_date < ? -- Parameter: requested end_date
      AND end_date > ?   -- Parameter: requested start_date
) AS is_available;

-- Performance: Uses idx_booking_property_dates_status partial index
-- Note: This detects overlapping date ranges efficiently
```

### 3. Create Payment Record When Booking is Finished

```sql
-- Use case: Application logic creates payment when booking status changes to FINISHED
-- This is typically done in a transaction with the booking status update

BEGIN;

-- 1. Update booking status to FINISHED
UPDATE referrals.booking
SET status = 'FINISHED',
    updated_at = CURRENT_TIMESTAMP
WHERE booking_id = ?
  AND status = 'RESERVED';

-- 2. Get referring user and commission rate from subscription
WITH booking_info AS (
    SELECT
        b.booking_id,
        b.property_id,
        b.total_amount,
        b.currency,
        rl.user_id,
        sp.commission_rate
    FROM referrals.booking b
    INNER JOIN referrals.referral_link rl ON b.referral_link_id = rl.id
    INNER JOIN subscriptions.user_subscription us ON rl.user_id = us.user_id
    INNER JOIN subscriptions.subscription_plan sp ON us.subscription_plan_id = sp.id
    WHERE b.booking_id = ?
      AND us.status = 'ACTIVE'
)
-- 3. Create payment record
INSERT INTO payment.booking (
    booking_id,
    user_id,
    property_id,
    percentage_commission,
    commission_amount,
    currency,
    payment_status
)
SELECT
    booking_id,
    user_id,
    property_id,
    commission_rate AS percentage_commission,
    ROUND(total_amount * (commission_rate / 100.0), 2) AS commission_amount,
    currency,
    'PENDING'
FROM booking_info;

COMMIT;

-- Note: Wrap in application transaction to ensure atomicity
```

### 4. Get User Earnings Summary

```sql
-- Use case: Dashboard showing total earnings, pending, and completed payments

SELECT
    u.id AS user_id,
    u.email,
    u.first_name || ' ' || u.last_name AS full_name,
    COUNT(CASE WHEN pb.payment_status = 'SUCCESS' THEN 1 END) AS successful_payments,
    COUNT(CASE WHEN pb.payment_status IN ('PENDING', 'PROCESSING') THEN 1 END) AS pending_payments,
    COALESCE(SUM(CASE WHEN pb.payment_status = 'SUCCESS' THEN pb.commission_amount END), 0) AS total_earned,
    COALESCE(SUM(CASE WHEN pb.payment_status IN ('PENDING', 'PROCESSING') THEN pb.commission_amount END), 0) AS pending_amount,
    pb.currency
FROM users.users u
LEFT JOIN payment.booking pb ON u.id = pb.user_id
WHERE u.id = ? -- Parameter: user ID
GROUP BY u.id, u.email, u.first_name, u.last_name, pb.currency;

-- Performance: Uses idx_payment_booking_user_id
```

### 5. Get Earnings History (Paginated)

```sql
-- Use case: Display user's earnings history with pagination

SELECT
    pb.payment_booking_id,
    pb.commission_amount,
    pb.currency,
    pb.payment_status,
    pb.paid_at,
    b.start_date,
    b.end_date,
    b.client_email,
    p.title AS property_title,
    p.location AS property_location
FROM payment.booking pb
INNER JOIN referrals.booking b ON pb.booking_id = b.booking_id
INNER JOIN properties.properties p ON pb.property_id = p.id
WHERE pb.user_id = ? -- Parameter: user ID
  AND pb.payment_status = 'SUCCESS'
ORDER BY pb.paid_at DESC
LIMIT ? OFFSET ?; -- Pagination parameters

-- Performance: Uses idx_payment_booking_user_success partial index
```

### 6. Find Pending Payments for Batch Processing

```sql
-- Use case: Scheduled job to process pending payments (runs every 15 minutes)

SELECT
    pb.payment_booking_id,
    pb.booking_id,
    pb.user_id,
    pb.commission_amount,
    pb.currency,
    pb.created_at,
    u.email AS user_email,
    u.first_name || ' ' || u.last_name AS user_name
FROM payment.booking pb
INNER JOIN users.users u ON pb.user_id = u.id
WHERE pb.payment_status = 'PENDING'
  AND pb.created_at <= CURRENT_TIMESTAMP - INTERVAL '5 minutes' -- Grace period
ORDER BY pb.created_at ASC
LIMIT 100; -- Process in batches

-- Performance: Uses idx_payment_booking_pending partial index
```

### 7. Monthly Revenue Report by Property

```sql
-- Use case: Admin dashboard - monthly revenue breakdown by property

SELECT
    p.id AS property_id,
    p.title AS property_title,
    DATE_TRUNC('month', b.created_at) AS month,
    COUNT(b.booking_id) AS total_bookings,
    COUNT(CASE WHEN b.status = 'FINISHED' THEN 1 END) AS completed_bookings,
    COUNT(CASE WHEN b.status = 'CANCELED' THEN 1 END) AS canceled_bookings,
    COALESCE(SUM(CASE WHEN b.status = 'FINISHED' THEN b.total_amount END), 0) AS total_revenue,
    COALESCE(SUM(pb.commission_amount), 0) AS total_commissions_paid,
    b.currency
FROM properties.properties p
LEFT JOIN referrals.booking b ON p.id = b.property_id
LEFT JOIN payment.booking pb ON b.booking_id = pb.booking_id AND pb.payment_status = 'SUCCESS'
WHERE b.created_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months'
GROUP BY p.id, p.title, month, b.currency
ORDER BY month DESC, total_revenue DESC;

-- Performance: May require composite index on (property_id, created_at) for large datasets
```

### 8. Detect Booking Anomalies (Same Client, Multiple Bookings)

```sql
-- Use case: Fraud detection - find clients with multiple bookings from different referral links

SELECT
    b.client_email,
    b.client_first_name,
    b.client_last_name,
    COUNT(DISTINCT b.referral_link_id) AS different_referral_links,
    COUNT(b.booking_id) AS total_bookings,
    ARRAY_AGG(DISTINCT rl.user_id) AS referring_users
FROM referrals.booking b
INNER JOIN referrals.referral_link rl ON b.referral_link_id = rl.id
WHERE b.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY b.client_email, b.client_first_name, b.client_last_name
HAVING COUNT(DISTINCT b.referral_link_id) > 1
ORDER BY total_bookings DESC;

-- Performance: Full table scan on recent bookings (acceptable for admin reports)
```

---

## Migration Execution

### Execution Order

The migrations MUST be executed in this order:

1. **V1.5__create_booking_enums.sql** - Create ENUM types
2. **V1.6__create_bookings_table.sql** - Create referrals.booking table
3. **V1.7__create_payment_schema.sql** - Create payment schema
4. **V1.8__create_payment_booking_table.sql** - Create payment.booking table

### Running Migrations

#### Automatic (Recommended)

Flyway runs automatically on Spring Boot application startup:

```bash
cd GydiMicroservices
./mvnw spring-boot:run
```

Flyway will detect new migrations and apply them in order.

#### Manual (for testing)

Using Flyway CLI:

```bash
cd GydiMicroservices
./mvnw flyway:info      # Check migration status
./mvnw flyway:migrate   # Apply pending migrations
./mvnw flyway:validate  # Validate applied migrations
```

#### Verification

After migration, verify:

```sql
-- Check Flyway history
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version >= '1.5'
ORDER BY installed_rank;

-- Verify tables exist
\dt referrals.*
\dt payment.*

-- Verify ENUM types
\dT+ referrals.booking_status
\dT+ payment.payment_status

-- Check indexes
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname IN ('referrals', 'payment')
ORDER BY tablename, indexname;
```

### Rollback Strategy

Flyway does NOT support automatic rollback. For rollback:

1. **Create new migration** to undo changes (e.g., `V1.9__rollback_payment_system.sql`)
2. **Manual rollback** (use rollback scripts in migration comments)

Example rollback for all migrations:

```sql
-- V1.9__rollback_payment_system.sql
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

COMMIT;
```

⚠️ **WARNING**: Only rollback in non-production environments or if no data exists.

---

## Common Operations

### 1. Create a Booking Request

```sql
INSERT INTO referrals.booking (
    referral_link_id,
    property_id,
    start_date,
    end_date,
    client_email,
    client_first_name,
    client_last_name,
    client_phone,
    total_amount,
    currency,
    status
) VALUES (
    1,                           -- referral_link_id
    5,                           -- property_id
    '2025-12-01',                -- start_date
    '2025-12-07',                -- end_date
    'john.doe@example.com',      -- client_email
    'John',                      -- client_first_name
    'Doe',                       -- client_last_name
    '+1234567890',               -- client_phone
    840.00,                      -- total_amount (6 nights × $140/night)
    'USD',                       -- currency
    'REQUEST'                    -- status
) RETURNING booking_id;
```

### 2. Confirm Booking (Owner Accepts)

```sql
UPDATE referrals.booking
SET status = 'RESERVED',
    updated_at = CURRENT_TIMESTAMP
WHERE booking_id = 1
  AND status = 'REQUEST'; -- Only allow transition from REQUEST

-- Returns 1 if successful, 0 if booking wasn't in REQUEST state
```

### 3. Cancel Booking

```sql
UPDATE referrals.booking
SET status = 'CANCELED',
    cancellation_reason = 'Client changed travel plans',
    canceled_by = 'CLIENT',
    canceled_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE booking_id = 1
  AND status IN ('REQUEST', 'RESERVED'); -- Cannot cancel FINISHED bookings
```

### 4. Complete Booking and Create Payment

```sql
BEGIN;

-- Step 1: Mark booking as finished
UPDATE referrals.booking
SET status = 'FINISHED',
    updated_at = CURRENT_TIMESTAMP
WHERE booking_id = 1
  AND status = 'RESERVED';

-- Step 2: Create payment record
INSERT INTO payment.booking (
    booking_id,
    user_id,
    property_id,
    percentage_commission,
    commission_amount,
    currency,
    payment_status
)
SELECT
    b.booking_id,
    rl.user_id,
    b.property_id,
    sp.commission_rate,
    ROUND(b.total_amount * (sp.commission_rate / 100.0), 2),
    b.currency,
    'PENDING'
FROM referrals.booking b
INNER JOIN referrals.referral_link rl ON b.referral_link_id = rl.id
INNER JOIN subscriptions.user_subscription us ON rl.user_id = us.user_id
INNER JOIN subscriptions.subscription_plan sp ON us.subscription_plan_id = sp.id
WHERE b.booking_id = 1
  AND us.status = 'ACTIVE';

COMMIT;
```

### 5. Process Payment (Mark as Success)

```sql
UPDATE payment.booking
SET payment_status = 'SUCCESS',
    payment_method = 'STRIPE',
    transaction_id = 'txn_1234567890abcdef',
    gateway_name = 'Stripe',
    gateway_response = '{"id": "txn_1234567890abcdef", "amount": 4200, "status": "succeeded"}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
    -- paid_at is auto-set by trigger when status changes to SUCCESS
WHERE payment_booking_id = 1
  AND payment_status IN ('PENDING', 'PROCESSING');
```

### 6. Get Booking Details with Payment Info

```sql
SELECT
    b.booking_id,
    b.start_date,
    b.end_date,
    b.client_email,
    b.total_amount,
    b.status AS booking_status,
    p.title AS property_title,
    rl.encrypted_link AS referral_link,
    u.email AS referring_user_email,
    pb.commission_amount,
    pb.percentage_commission,
    pb.payment_status,
    pb.paid_at
FROM referrals.booking b
INNER JOIN properties.properties p ON b.property_id = p.id
INNER JOIN referrals.referral_link rl ON b.referral_link_id = rl.id
INNER JOIN users.users u ON rl.user_id = u.id
LEFT JOIN payment.booking pb ON b.booking_id = pb.booking_id
WHERE b.booking_id = ?;
```

---

## Performance Considerations

### Query Optimization

1. **Use Partial Indexes for Filtered Queries**
   - Example: `idx_booking_referral_active` only indexes active bookings
   - Reduces index size by 50-70% in typical workloads
   - Faster writes (fewer index updates)

2. **Leverage Composite Indexes**
   - `idx_booking_property_dates_status` combines 4 columns for availability checks
   - Avoids multiple index scans
   - Critical for preventing double bookings

3. **EXPLAIN ANALYZE Your Queries**
   - Always check execution plans for slow queries
   - Look for sequential scans on large tables
   - Verify index usage

   ```sql
   EXPLAIN (ANALYZE, BUFFERS) SELECT ...;
   ```

4. **Connection Pooling**
   - Use HikariCP (already configured in Spring Boot)
   - Recommended pool size: `connections = ((core_count * 2) + effective_spindle_count)`
   - For PostgreSQL 16: typically 10-20 connections per application instance

### Data Volume Considerations

**Current Design Supports:**
- **Bookings**: 10M+ rows (BIGSERIAL primary key)
- **Payments**: 10M+ rows (BIGSERIAL primary key)

**When to Consider Partitioning:**
- **Table Size > 100M rows**: Partition by date (monthly/yearly)
- **Hot vs Cold Data**: Older bookings accessed less frequently

Example partitioning strategy (future):

```sql
-- Partition bookings by created_at (monthly)
CREATE TABLE referrals.booking (
    -- same columns
) PARTITION BY RANGE (created_at);

CREATE TABLE referrals.booking_2025_12 PARTITION OF referrals.booking
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
```

### Write Performance

1. **Batch Processing**
   - Process pending payments in batches of 100
   - Use `LIMIT 100` in batch jobs

2. **Avoid Lock Contention**
   - Keep transactions short
   - Update booking status and create payment in same transaction (atomicity)
   - Use `FOR UPDATE SKIP LOCKED` for concurrent batch processing

3. **Index Maintenance**
   - Monitor index bloat: `SELECT pg_size_pretty(pg_relation_size('index_name'));`
   - Rebuild indexes periodically: `REINDEX INDEX CONCURRENTLY idx_name;`

---

## Future Extensibility

### 1. Payment Gateway Integration

The schema is designed to support multiple payment gateways:

**Current Extensibility:**
- `payment_method` VARCHAR(50) - supports any gateway name
- `gateway_response` JSONB - stores full gateway response
- `transaction_id` VARCHAR(255) - supports various ID formats

**Adding New Gateway:**

```sql
-- Example: Adding PayPal payment
UPDATE payment.booking
SET payment_status = 'SUCCESS',
    payment_method = 'PAYPAL',
    transaction_id = 'PAY-12345678901234567',
    gateway_name = 'PayPal',
    gateway_response = '{"paypal_data": "..."}'::jsonb
WHERE payment_booking_id = 1;

-- No schema changes needed!
```

### 2. Refund Support

Add columns for refund tracking:

```sql
-- V1.9__add_refund_support.sql
ALTER TABLE payment.booking
ADD COLUMN refund_status VARCHAR(20),
ADD COLUMN refund_amount DECIMAL(10, 2),
ADD COLUMN refund_reason TEXT,
ADD COLUMN refunded_at TIMESTAMP WITH TIME ZONE,
ADD CONSTRAINT chk_refund_status
    CHECK (refund_status IN ('NONE', 'PARTIAL', 'FULL')),
ADD CONSTRAINT chk_refund_amount
    CHECK (refund_amount IS NULL OR refund_amount >= 0);
```

### 3. Multi-Currency Support

Current design supports multiple currencies:
- `currency` VARCHAR(3) column in both tables
- Stored as ISO 4217 codes (USD, EUR, GBP, etc.)

**For Exchange Rate Tracking:**

```sql
-- V1.10__add_exchange_rates.sql
CREATE TABLE payment.exchange_rate (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(12, 6) NOT NULL,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exchange_rate_positive CHECK (rate > 0),
    UNIQUE (from_currency, to_currency, effective_date)
);
```

### 4. Installment Payments

For bookings with installment plans:

```sql
-- V1.11__add_installment_payments.sql
CREATE TABLE payment.installment (
    installment_id BIGSERIAL PRIMARY KEY,
    payment_booking_id BIGINT NOT NULL REFERENCES payment.booking(payment_booking_id),
    installment_number INT NOT NULL,
    installment_amount DECIMAL(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_installment_number CHECK (installment_number > 0),
    CONSTRAINT chk_installment_amount CHECK (installment_amount > 0),
    UNIQUE (payment_booking_id, installment_number)
);
```

### 5. Booking Reviews/Ratings

Link bookings to reviews:

```sql
-- V1.12__add_booking_reviews.sql
CREATE TABLE referrals.booking_review (
    review_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES referrals.booking(booking_id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 6. Audit Trail Enhancement

Track who made changes (for compliance):

```sql
-- V1.13__add_audit_fields.sql
ALTER TABLE referrals.booking
ADD COLUMN created_by BIGINT REFERENCES users.users(id),
ADD COLUMN updated_by BIGINT REFERENCES users.users(id);

ALTER TABLE payment.booking
ADD COLUMN created_by BIGINT REFERENCES users.users(id),
ADD COLUMN updated_by BIGINT REFERENCES users.users(id);

-- Update trigger to track updated_by
CREATE OR REPLACE FUNCTION referrals.track_booking_changes()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    -- Application must set updated_by via SET LOCAL
    NEW.updated_by = COALESCE(
        NULLIF(current_setting('app.current_user_id', true), '')::BIGINT,
        NEW.updated_by
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## Troubleshooting

### Issue: Migration Fails with "relation already exists"

**Cause**: Migration was previously applied or table exists from manual creation.

**Solution:**
```sql
-- Check Flyway history
SELECT * FROM flyway_schema_history WHERE version >= '1.5';

-- If migration is marked as failed, repair it
./mvnw flyway:repair

-- If table exists but migration didn't run, manually mark as applied
INSERT INTO flyway_schema_history (version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES ('1.5', 'create_booking_enums', 'SQL', 'V1.5__create_booking_enums.sql', -1, 'manual', NOW(), 0, true);
```

### Issue: Foreign Key Constraint Violation

**Cause**: Trying to insert booking with non-existent referral_link_id or property_id.

**Solution:**
```sql
-- Verify foreign key data exists
SELECT id FROM referrals.referral_link WHERE id = ?;
SELECT id FROM properties.properties WHERE id = ?;

-- If missing, create parent records first
```

### Issue: Payment Record Creation Fails

**Cause**: Booking status is not 'FINISHED' or user has no active subscription.

**Solution:**
```sql
-- Check booking status
SELECT booking_id, status FROM referrals.booking WHERE booking_id = ?;

-- Check user subscription
SELECT us.status, sp.commission_rate
FROM referrals.referral_link rl
INNER JOIN subscriptions.user_subscription us ON rl.user_id = us.user_id
INNER JOIN subscriptions.subscription_plan sp ON us.subscription_plan_id = sp.id
WHERE rl.id = (SELECT referral_link_id FROM referrals.booking WHERE booking_id = ?);
```

### Issue: Slow Query Performance

**Cause**: Missing index or inefficient query structure.

**Solution:**
```sql
-- Analyze query
EXPLAIN (ANALYZE, BUFFERS) SELECT ...;

-- Check for sequential scans
-- Look for "Seq Scan on table_name"

-- Verify index usage
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE schemaname IN ('referrals', 'payment')
ORDER BY idx_scan ASC;

-- If index exists but not used, check statistics
ANALYZE referrals.booking;
ANALYZE payment.booking;
```

---

## Summary

This booking and payment schema provides:

✅ **Data Integrity**: Comprehensive constraints enforce business rules at database level
✅ **Performance**: Strategic indexes for common query patterns
✅ **Audit Trail**: Full timestamp tracking with automatic triggers
✅ **Extensibility**: JSONB fields and nullable columns for future enhancements
✅ **Scalability**: BIGSERIAL keys support 10M+ rows per table
✅ **Maintainability**: Clear separation of booking and payment domains

**Next Steps:**
1. Run migrations: `./mvnw spring-boot:run`
2. Verify schema: Check Flyway history and table structure
3. Implement JPA entities in backend (hexagonal architecture)
4. Create use cases for booking lifecycle
5. Build REST API endpoints
6. Integrate payment gateway (Stripe/PayPal)
7. Add monitoring and alerting for payment processing

---

**Documentation Version:** 1.0
**Last Updated:** 2025-11-24
**Author:** Database Architect
**Review Status:** Ready for Implementation
