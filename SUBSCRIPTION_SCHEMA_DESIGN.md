# Subscription System - Database Schema Design

**Author**: Database Architect
**Date**: 2025-12-09
**Version**: 1.0
**Status**: Ready for Implementation

---

## Executive Summary

This document describes the complete database schema design for the GYDI 2.0 subscription management system. The design supports three subscription plans (FREE, PRO, ELITE) with commission-based referral earnings, payment method management, and comprehensive transaction audit trails.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Subscription Plans](#subscription-plans)
3. [Schema Design](#schema-design)
4. [Migration Files](#migration-files)
5. [Use Cases](#use-cases)
6. [Performance Considerations](#performance-considerations)
7. [Security Considerations](#security-considerations)
8. [Upgrade/Downgrade Strategy](#upgradedowngrade-strategy)
9. [Testing Strategy](#testing-strategy)
10. [Rollback Plan](#rollback-plan)

---

## System Overview

### Subscription Plans

| Plan | Monthly Price | Commission Rate | Referral Limit | Property Limit |
|------|--------------|-----------------|----------------|----------------|
| **FREE** | $0 | 2% | 50/month | 10 |
| **PRO** | $29.99 | 5% | 200/month | 50 |
| **ELITE** | $99.99 | 10% | Unlimited | Unlimited |

### Key Features

- **User Registration**: All new users start with FREE plan (default)
- **Upgrade**: Users can upgrade from FREE → PRO → ELITE
- **Downgrade**: Users can downgrade to any lower-tier plan
- **Auto-Renewal**: Paid plans (PRO, ELITE) can auto-renew monthly
- **Payment Methods**: Users can store multiple tokenized payment methods
- **Transaction History**: Complete audit trail of all plan changes and payments
- **Backward Compatibility**: Syncs with existing `users.active_plan` column

---

## Schema Design

### Entity Relationship Diagram (ERD)

```
┌────────────────────────────────────────────────────────────────┐
│                     subscriptions SCHEMA                        │
└────────────────────────────────────────────────────────────────┘

users.users (1) ───────< (N) user_subscriptions
                                    │
                                    │ (N:1)
                                    ▼
                         subscription_plans (catalog)

users.users (1) ───────< (N) payment_methods

user_subscriptions (1) ───< (N) subscription_transactions
payment_methods (0..1) ───< (N) subscription_transactions
```

### Table Definitions

#### 1. `subscription_plans` (Catalog Table)

**Purpose**: Catalog of available subscription plans (relatively static)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Plan ID |
| `plan_code` | VARCHAR(20) | NOT NULL, UNIQUE | Plan code (FREE, PRO, ELITE) |
| `plan_name` | VARCHAR(100) | NOT NULL | Display name |
| `plan_description` | TEXT | | Marketing description |
| `monthly_price` | DECIMAL(10,2) | NOT NULL, >= 0 | Monthly price in USD |
| `currency` | VARCHAR(3) | NOT NULL | Currency code (USD) |
| `commission_rate` | DECIMAL(5,4) | NOT NULL, 0-1 | Commission rate (0.02 = 2%) |
| `referral_limit_per_month` | INT | NOT NULL, >= 0 | Max referrals per month |
| `property_publish_limit` | INT | NOT NULL, >= 0 | Max properties |
| `is_active` | BOOLEAN | NOT NULL | Can users subscribe? |
| `is_featured` | BOOLEAN | NOT NULL | Show as featured? |
| `display_order` | INT | NOT NULL | Sort order |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Last update timestamp |

**Indexes**:
- `idx_subscription_plans_active`: (is_active, display_order) WHERE is_active = TRUE
- `idx_subscription_plans_code`: (plan_code)

**Business Rules**:
- `plan_code` must be uppercase
- FREE plan must have `monthly_price = 0`
- Paid plans must have `monthly_price > 0`

---

#### 2. `user_subscriptions` (Active Subscriptions)

**Purpose**: Tracks the current active subscription for each user (1:1 relationship)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Subscription ID |
| `user_id` | BIGINT | NOT NULL, UNIQUE, FK | User ID (one subscription per user) |
| `plan_id` | BIGINT | NOT NULL, FK | Current plan ID |
| `started_at` | TIMESTAMPTZ | NOT NULL | Subscription start date |
| `expires_at` | TIMESTAMPTZ | | Expiration date (NULL for FREE) |
| `status` | ENUM | NOT NULL | ACTIVE, EXPIRED, CANCELED, SUSPENDED |
| `auto_renew` | BOOLEAN | NOT NULL | Auto-renew on expiration? |
| `payment_method_id` | BIGINT | FK | Default payment method |
| `canceled_at` | TIMESTAMPTZ | | Cancellation timestamp |
| `cancellation_reason` | TEXT | | User-provided reason |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Last update timestamp |

**Indexes**:
- `idx_user_subscriptions_user`: (user_id, status)
- `idx_user_subscriptions_expiring`: (expires_at, status, auto_renew) WHERE status = 'ACTIVE' AND expires_at IS NOT NULL
- `idx_user_subscriptions_plan`: (plan_id, status)

**Business Rules**:
- Each user can have exactly **one** subscription (enforced by UNIQUE constraint on `user_id`)
- FREE plan: `expires_at` must be NULL (never expires)
- Paid plans: `expires_at` must be NOT NULL
- If `status = CANCELED`, `canceled_at` must be NOT NULL
- If `auto_renew = TRUE`, `payment_method_id` must be NOT NULL

---

#### 3. `payment_methods` (Tokenized Payment Methods)

**Purpose**: Stores tokenized payment methods (cards, PayPal) for users

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Payment method ID |
| `user_id` | BIGINT | NOT NULL, FK | User ID |
| `method_type` | ENUM | NOT NULL | CREDIT_CARD, DEBIT_CARD, PAYPAL, etc. |
| `gateway_provider` | VARCHAR(50) | NOT NULL | stripe, paypal, etc. |
| `gateway_token` | VARCHAR(255) | NOT NULL | Token from gateway (e.g., Stripe PM ID) |
| `card_last_four` | CHAR(4) | | Last 4 digits (display only) |
| `card_brand` | VARCHAR(50) | | Visa, Mastercard, Amex, etc. |
| `card_expiry_month` | INT | 1-12 | Card expiry month |
| `card_expiry_year` | INT | >= 2025 | Card expiry year |
| `paypal_email` | VARCHAR(255) | | PayPal email (if applicable) |
| `billing_name` | VARCHAR(255) | | Billing name |
| `billing_country` | VARCHAR(2) | | ISO country code |
| `billing_postal_code` | VARCHAR(20) | | Postal code |
| `is_default` | BOOLEAN | NOT NULL | Default payment method? |
| `is_active` | BOOLEAN | NOT NULL | Can be used? |
| `deleted_at` | TIMESTAMPTZ | | Soft delete timestamp |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Last update timestamp |

**Indexes**:
- `idx_payment_methods_user`: (user_id, is_active, deleted_at)
- `idx_payment_methods_default`: (user_id, is_default) WHERE is_default = TRUE AND deleted_at IS NULL
- `idx_payment_methods_expiring`: (card_expiry_year, card_expiry_month) WHERE deleted_at IS NULL

**Business Rules**:
- Multiple payment methods per user allowed
- Only one default payment method per user (enforced by trigger)
- **NEVER** store raw card numbers, only tokens
- Soft delete only (physical deletion prohibited for audit)
- Credit/Debit cards must have `card_last_four`
- PayPal methods must have `paypal_email`

---

#### 4. `subscription_transactions` (Audit Log)

**Purpose**: Immutable audit log of all subscription-related transactions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Transaction ID |
| `user_subscription_id` | BIGINT | NOT NULL, FK | Subscription ID |
| `user_id` | BIGINT | NOT NULL, FK | User ID (denormalized) |
| `from_plan_id` | BIGINT | FK | Previous plan (NULL for initial) |
| `to_plan_id` | BIGINT | NOT NULL, FK | New plan |
| `payment_method_id` | BIGINT | FK | Payment method used |
| `transaction_type` | ENUM | NOT NULL | INITIAL_SUBSCRIPTION, UPGRADE, RENEWAL, DOWNGRADE, CANCELLATION, REACTIVATION |
| `transaction_status` | ENUM | NOT NULL | PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELED |
| `amount` | DECIMAL(10,2) | NOT NULL, >= 0 | Amount charged (0 for FREE) |
| `currency` | VARCHAR(3) | NOT NULL | Currency code |
| `gateway_provider` | VARCHAR(50) | | Payment gateway |
| `gateway_transaction_id` | VARCHAR(255) | | Gateway transaction ID |
| `gateway_response` | JSONB | | Full gateway response |
| `period_start` | TIMESTAMPTZ | NOT NULL | Subscription period start |
| `period_end` | TIMESTAMPTZ | | Subscription period end |
| `failure_reason` | TEXT | | Failure reason (if failed) |
| `retry_count` | INT | NOT NULL | Retry attempts |
| `metadata` | JSONB | | Additional data (promo codes, etc.) |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `completed_at` | TIMESTAMPTZ | | Completion timestamp |

**Indexes**:
- `idx_subscription_transactions_user`: (user_id, created_at DESC)
- `idx_subscription_transactions_subscription`: (user_subscription_id, created_at DESC)
- `idx_subscription_transactions_status`: (transaction_status, created_at) WHERE transaction_status = 'PENDING'
- `idx_subscription_transactions_gateway`: (gateway_provider, gateway_transaction_id) WHERE gateway_transaction_id IS NOT NULL
- `idx_subscription_transactions_type`: (transaction_type, created_at DESC)

**Business Rules**:
- **IMMUTABLE**: Records cannot be modified after creation (financial audit trail)
- If `transaction_status = COMPLETED`, `completed_at` must be NOT NULL
- FREE plan transactions must have `amount = 0`
- Paid plan transactions must have `payment_method_id` NOT NULL

---

## Migration Files

### Migration Order

1. **V51__create_subscriptions_schema.sql**
   - Creates `subscriptions` schema
   - Creates ENUMs: `subscription_status`, `transaction_type`, `transaction_status`, `payment_method_type`

2. **V52__create_subscription_plans_and_tables.sql**
   - Creates 4 main tables: `subscription_plans`, `user_subscriptions`, `payment_methods`, `subscription_transactions`
   - Creates indexes for performance
   - Creates triggers for `updated_at`
   - Creates helper functions: `is_subscription_expired()`, `auto_expire_subscriptions()`, `get_user_plan()`

3. **V53__seed_subscription_plans.sql**
   - Inserts 3 plans: FREE, PRO, ELITE
   - Creates FREE subscriptions for existing users
   - Syncs `users.active_plan` column with new schema
   - Creates trigger to keep `users.active_plan` in sync

### Running Migrations

```bash
cd GydiMicroservices
./mvnw spring-boot:run
# Flyway will automatically apply migrations on startup
```

### Verification Queries

```sql
-- 1. Check all plans exist
SELECT * FROM subscriptions.subscription_plans ORDER BY display_order;

-- 2. Check all users have subscriptions
SELECT COUNT(*) AS users_with_subscriptions
FROM subscriptions.user_subscriptions;

-- 3. Check plan distribution
SELECT sp.plan_code, COUNT(*) AS user_count
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
GROUP BY sp.plan_code
ORDER BY sp.display_order;

-- 4. Verify users.active_plan is in sync
SELECT u.id, u.email, u.active_plan AS old_plan, sp.plan_code AS new_plan
FROM users.users u
JOIN subscriptions.user_subscriptions us ON u.id = us.user_id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE u.active_plan != sp.plan_code;  -- Should return 0 rows
```

---

## Use Cases

### 1. User Registration (FREE Plan by Default)

**Flow**:
1. New user registers via `/api/auth/register`
2. Application creates user in `users.users` table
3. Trigger or application logic creates FREE subscription in `user_subscriptions`
4. Transaction recorded in `subscription_transactions` with type `INITIAL_SUBSCRIPTION`

**SQL Example**:
```sql
-- Insert user (application code)
INSERT INTO users.users (email, password_hash, ...) VALUES (...);

-- Create FREE subscription (application code or trigger)
INSERT INTO subscriptions.user_subscriptions (user_id, plan_id, status)
VALUES (
    <user_id>,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    'ACTIVE'
);

-- Record transaction
INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    to_plan_id,
    transaction_type,
    transaction_status,
    amount,
    period_start
) VALUES (
    <subscription_id>,
    <user_id>,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    'INITIAL_SUBSCRIPTION',
    'COMPLETED',
    0.00,
    CURRENT_TIMESTAMP
);
```

---

### 2. Upgrade (FREE → PRO / ELITE)

**Flow**:
1. User selects PRO or ELITE plan on frontend
2. User adds/selects payment method
3. Application charges payment method via Stripe
4. If payment successful:
   - Update `user_subscriptions.plan_id` to new plan
   - Set `expires_at` to 30 days from now
   - Set `auto_renew = TRUE`
   - Record transaction in `subscription_transactions`

**SQL Example**:
```sql
-- Update subscription
UPDATE subscriptions.user_subscriptions
SET
    plan_id = (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'PRO'),
    expires_at = CURRENT_TIMESTAMP + INTERVAL '30 days',
    auto_renew = TRUE,
    payment_method_id = <payment_method_id>,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = <user_id>;

-- Record transaction
INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    payment_method_id,
    transaction_type,
    transaction_status,
    amount,
    gateway_provider,
    gateway_transaction_id,
    period_start,
    period_end,
    completed_at
) VALUES (
    <subscription_id>,
    <user_id>,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'PRO'),
    <payment_method_id>,
    'UPGRADE',
    'COMPLETED',
    29.99,
    'stripe',
    '<stripe_charge_id>',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP
);
```

---

### 3. Renewal (Auto-Renew PRO / ELITE)

**Flow**:
1. Scheduled job runs daily (cron): checks for subscriptions expiring in next 7 days
2. For each subscription with `auto_renew = TRUE`:
   - Charge payment method via Stripe
   - If successful: extend `expires_at` by 30 days
   - If failed: mark subscription as `SUSPENDED`, notify user
3. Record transaction in `subscription_transactions`

**SQL Example**:
```sql
-- Find subscriptions expiring in next 7 days
SELECT us.id, us.user_id, us.payment_method_id, sp.monthly_price
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE'
  AND us.auto_renew = TRUE
  AND us.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days';

-- After successful payment
UPDATE subscriptions.user_subscriptions
SET
    expires_at = expires_at + INTERVAL '30 days',
    updated_at = CURRENT_TIMESTAMP
WHERE id = <subscription_id>;

-- Record transaction
INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    payment_method_id,
    transaction_type,
    transaction_status,
    amount,
    gateway_provider,
    gateway_transaction_id,
    period_start,
    period_end,
    completed_at
) VALUES (
    <subscription_id>,
    <user_id>,
    <current_plan_id>,
    <current_plan_id>,  -- Same plan for renewal
    <payment_method_id>,
    'RENEWAL',
    'COMPLETED',
    29.99,
    'stripe',
    '<stripe_charge_id>',
    <current_expires_at>,
    <new_expires_at>,
    CURRENT_TIMESTAMP
);
```

---

### 4. Downgrade (PRO → FREE / ELITE → PRO)

**Flow**:
1. User requests downgrade (e.g., PRO → FREE)
2. Application immediately:
   - Updates `user_subscriptions.plan_id` to FREE
   - Sets `expires_at = NULL` (FREE never expires)
   - Sets `auto_renew = FALSE`
   - Records transaction
3. **Note**: No refund for remaining days (user keeps paid plan until expiration)

**Alternative Strategy (Deferred Downgrade)**:
- Schedule downgrade for end of current billing period
- Add `pending_plan_id` column to track scheduled changes

**SQL Example**:
```sql
-- Immediate downgrade
UPDATE subscriptions.user_subscriptions
SET
    plan_id = (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    expires_at = NULL,
    auto_renew = FALSE,
    payment_method_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = <user_id>;

-- Record transaction
INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    transaction_type,
    transaction_status,
    amount,
    period_start,
    completed_at
) VALUES (
    <subscription_id>,
    <user_id>,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'PRO'),
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    'DOWNGRADE',
    'COMPLETED',
    0.00,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

---

### 5. Cancellation (Cancel Paid Plan)

**Flow**:
1. User cancels subscription
2. Application:
   - Sets `status = CANCELED`
   - Sets `auto_renew = FALSE`
   - Sets `canceled_at = CURRENT_TIMESTAMP`
   - Records cancellation reason
3. Subscription remains active until `expires_at` (user gets remaining days)
4. At expiration, scheduled job downgrades to FREE plan

**SQL Example**:
```sql
-- Cancel subscription
UPDATE subscriptions.user_subscriptions
SET
    status = 'CANCELED',
    auto_renew = FALSE,
    canceled_at = CURRENT_TIMESTAMP,
    cancellation_reason = '<user_provided_reason>',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = <user_id>;

-- Record transaction
INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    transaction_type,
    transaction_status,
    amount,
    completed_at
) VALUES (
    <subscription_id>,
    <user_id>,
    <current_plan_id>,
    <current_plan_id>,  -- Still on same plan until expiration
    'CANCELLATION',
    'COMPLETED',
    0.00,
    CURRENT_TIMESTAMP
);
```

---

### 6. Add Payment Method

**Flow**:
1. User adds credit card on frontend
2. Frontend tokenizes card via Stripe.js (client-side, PCI compliant)
3. Application receives Stripe payment method token
4. Application stores token in `payment_methods` table
5. If this is user's first payment method, set `is_default = TRUE`

**SQL Example**:
```sql
-- Insert payment method
INSERT INTO subscriptions.payment_methods (
    user_id,
    method_type,
    gateway_provider,
    gateway_token,
    card_last_four,
    card_brand,
    card_expiry_month,
    card_expiry_year,
    billing_name,
    billing_country,
    billing_postal_code,
    is_default,
    is_active
) VALUES (
    <user_id>,
    'CREDIT_CARD',
    'stripe',
    '<stripe_pm_id>',
    '4242',
    'Visa',
    12,
    2026,
    'John Doe',
    'US',
    '90210',
    TRUE,  -- Set as default if first payment method
    TRUE
);
```

---

## Performance Considerations

### 1. Indexing Strategy

**High-Frequency Queries**:
- Get user's current plan: `user_subscriptions(user_id, status)`
- Find expiring subscriptions: `user_subscriptions(expires_at, status, auto_renew)`
- User transaction history: `subscription_transactions(user_id, created_at DESC)`
- Get payment methods: `payment_methods(user_id, is_active, deleted_at)`

**Indexes Created**:
- All foreign keys are indexed
- Partial indexes for filtered queries (e.g., WHERE status = 'ACTIVE')
- Composite indexes for common query patterns

**Index Maintenance**:
```sql
-- Rebuild indexes monthly (or when fragmented)
REINDEX TABLE subscriptions.user_subscriptions;
REINDEX TABLE subscriptions.subscription_transactions;

-- Vacuum and analyze weekly
VACUUM ANALYZE subscriptions.user_subscriptions;
VACUUM ANALYZE subscriptions.subscription_transactions;
```

---

### 2. Query Optimization

**Example: Get user plan with details**
```sql
-- Optimized query (uses index on user_id, status)
SELECT
    u.id AS user_id,
    u.email,
    sp.plan_code,
    sp.plan_name,
    sp.monthly_price,
    sp.commission_rate,
    us.expires_at,
    us.status
FROM users.users u
JOIN subscriptions.user_subscriptions us ON u.id = us.user_id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE u.id = <user_id>
  AND us.status = 'ACTIVE';
```

**Explain Analyze**:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM subscriptions.get_user_plan(123);
```

---

### 3. Caching Strategy

**Application-Level Caching** (Redis/Caffeine):
- Cache `subscription_plans` (static, rarely changes) - TTL: 1 hour
- Cache user's current plan - TTL: 5 minutes
- Invalidate cache on plan change

**Example (Spring Cache)**:
```java
@Cacheable(value = "user_plans", key = "#userId")
public SubscriptionPlanDTO getUserPlan(Long userId) {
    // Database query
}

@CacheEvict(value = "user_plans", key = "#userId")
public void upgradePlan(Long userId, PlanCode newPlan) {
    // Update subscription
}
```

---

### 4. Scheduled Jobs (Cron)

**Daily Jobs**:
1. **Auto-expire subscriptions**:
   ```sql
   SELECT * FROM subscriptions.auto_expire_subscriptions();
   ```

2. **Process renewals** (7 days before expiration):
   ```java
   @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
   public void processRenewals() {
       // Find subscriptions expiring in 7 days
       // Charge payment methods
       // Update subscriptions
   }
   ```

3. **Notify expiring subscriptions** (send emails):
   ```sql
   SELECT u.email, us.expires_at, sp.plan_name
   FROM subscriptions.user_subscriptions us
   JOIN users.users u ON us.user_id = u.id
   JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
   WHERE us.status = 'ACTIVE'
     AND us.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days'
     AND us.auto_renew = FALSE;
   ```

---

## Security Considerations

### 1. Payment Method Security

**NEVER Store**:
- ❌ Raw credit card numbers
- ❌ CVV codes
- ❌ Full PAN (Primary Account Number)

**Always Store**:
- ✅ Tokenized payment method IDs (Stripe PM IDs)
- ✅ Last 4 digits only (for display)
- ✅ Card brand and expiry (for UX)

**Encryption**:
- Gateway tokens should be encrypted at rest (use PostgreSQL `pgcrypto`)
- Use application-level encryption for additional security

**Example**:
```sql
-- Encrypt gateway token on insert
INSERT INTO subscriptions.payment_methods (gateway_token)
VALUES (pgp_sym_encrypt('<stripe_pm_id>', '<encryption_key>'));

-- Decrypt on read
SELECT pgp_sym_decrypt(gateway_token::bytea, '<encryption_key>')
FROM subscriptions.payment_methods;
```

---

### 2. PCI Compliance

**Tokenization Flow**:
1. User enters card details on frontend
2. Frontend sends card data directly to Stripe (via Stripe.js)
3. Stripe returns payment method token
4. Backend receives token only (never raw card data)
5. Backend stores token in database

**This design ensures**:
- Application never touches raw card data
- PCI DSS compliance (SAQ A level)
- Reduced security liability

---

### 3. Access Control

**Database Permissions**:
```sql
-- Application user (least privilege)
GRANT SELECT, INSERT, UPDATE ON subscriptions.subscription_plans TO gydi_app_user;
GRANT SELECT, INSERT, UPDATE ON subscriptions.user_subscriptions TO gydi_app_user;
GRANT SELECT, INSERT, UPDATE ON subscriptions.payment_methods TO gydi_app_user;
GRANT SELECT, INSERT ON subscriptions.subscription_transactions TO gydi_app_user;  -- NO UPDATE/DELETE

-- Read-only user (reporting)
GRANT SELECT ON ALL TABLES IN SCHEMA subscriptions TO gydi_readonly_user;
```

**Row-Level Security (RLS)**:
```sql
-- Enable RLS on payment_methods
ALTER TABLE subscriptions.payment_methods ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own payment methods
CREATE POLICY payment_methods_user_isolation ON subscriptions.payment_methods
FOR SELECT
USING (user_id = current_setting('app.current_user_id')::BIGINT);
```

---

## Upgrade/Downgrade Strategy

### Upgrade Strategy (FREE → PRO → ELITE)

**Immediate Upgrade** (Recommended):
1. User selects higher-tier plan
2. Application charges **prorated amount** for current month:
   ```
   Prorated Amount = (New Plan Price) × (Days Remaining / 30)
   ```
3. Update subscription immediately
4. Set new `expires_at` = 30 days from now
5. User gets new features immediately

**Example Calculation**:
- User on FREE, upgrades to PRO ($29.99/month)
- 15 days remaining in month
- Prorated charge: $29.99 × (15/30) = **$14.99**

**SQL**:
```sql
UPDATE subscriptions.user_subscriptions
SET
    plan_id = <new_plan_id>,
    expires_at = CURRENT_TIMESTAMP + INTERVAL '30 days',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = <user_id>;
```

---

### Downgrade Strategy (ELITE → PRO → FREE)

**Option 1: Immediate Downgrade** (Simpler, No Refund):
- User downgrades immediately
- No refund for remaining days
- Simpler to implement

**Option 2: Deferred Downgrade** (Better UX):
- User keeps current plan until expiration
- Downgrade scheduled for end of billing period
- Better user experience (get what you paid for)

**Recommended: Option 2 (Deferred Downgrade)**

**Implementation**:
Add `pending_plan_id` column to `user_subscriptions`:
```sql
ALTER TABLE subscriptions.user_subscriptions
ADD COLUMN pending_plan_id BIGINT REFERENCES subscriptions.subscription_plans(id);
```

**Flow**:
1. User requests downgrade
2. Set `pending_plan_id` = FREE
3. At expiration, scheduled job:
   - Sets `plan_id = pending_plan_id`
   - Sets `pending_plan_id = NULL`
   - Sets `expires_at = NULL` (for FREE)

---

## Testing Strategy

### Unit Tests (JUnit 5)

**Test Coverage**:
- ✅ Plan validation (commission rates, limits)
- ✅ Subscription lifecycle (ACTIVE → EXPIRED → CANCELED)
- ✅ Payment method validation (tokenization, card expiry)
- ✅ Transaction immutability (cannot update/delete)

**Example**:
```java
@Test
void testUpgradeFromFreeToPro() {
    // Given: User with FREE plan
    var user = createUser();
    var subscription = createSubscription(user, PlanCode.FREE);

    // When: User upgrades to PRO
    subscriptionService.upgradePlan(user.getId(), PlanCode.PRO);

    // Then: Subscription is updated
    var updated = subscriptionRepository.findByUserId(user.getId());
    assertThat(updated.getPlan().getCode()).isEqualTo(PlanCode.PRO);
    assertThat(updated.getExpiresAt()).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
}
```

---

### Integration Tests (TestContainers)

**Test Database Setup**:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
    .withDatabaseName("gydi_test")
    .withUsername("test")
    .withPassword("test");

@Test
void testSubscriptionTransactionAudit() {
    // Given: User upgrades plan
    subscriptionService.upgradePlan(userId, PlanCode.PRO);

    // Then: Transaction is recorded
    var transactions = transactionRepository.findByUserId(userId);
    assertThat(transactions).hasSize(1);
    assertThat(transactions.get(0).getTransactionType())
        .isEqualTo(TransactionType.UPGRADE);
}
```

---

### Manual Testing Scenarios

1. **User Registration**:
   - ✅ User gets FREE plan by default
   - ✅ Transaction recorded with type INITIAL_SUBSCRIPTION

2. **Upgrade Flow**:
   - ✅ User can upgrade to PRO
   - ✅ Payment method is charged
   - ✅ Subscription updated with new expiration
   - ✅ Transaction recorded

3. **Renewal Flow**:
   - ✅ Subscription auto-renews 7 days before expiration
   - ✅ Payment method is charged
   - ✅ Expiration extended by 30 days

4. **Downgrade Flow**:
   - ✅ User can downgrade to FREE
   - ✅ Subscription updated immediately
   - ✅ Transaction recorded

5. **Payment Method Management**:
   - ✅ User can add multiple payment methods
   - ✅ User can set default payment method
   - ✅ User can delete payment method (soft delete)

---

## Rollback Plan

### Rollback Migrations

If migrations fail or cause issues, rollback manually:

```sql
-- Rollback V53 (seed data)
DELETE FROM subscriptions.user_subscriptions WHERE plan_id IN (
    SELECT id FROM subscriptions.subscription_plans
);
DELETE FROM subscriptions.subscription_plans;
DROP TRIGGER IF EXISTS sync_user_active_plan_trigger ON subscriptions.user_subscriptions;
DROP FUNCTION IF EXISTS subscriptions.sync_user_active_plan();

-- Rollback V52 (tables)
DROP FUNCTION IF EXISTS subscriptions.get_user_plan(BIGINT);
DROP FUNCTION IF EXISTS subscriptions.auto_expire_subscriptions();
DROP FUNCTION IF EXISTS subscriptions.is_subscription_expired(BIGINT);
DROP FUNCTION IF EXISTS subscriptions.ensure_single_default_payment_method();
DROP FUNCTION IF EXISTS subscriptions.update_updated_at_column();
DROP TABLE IF EXISTS subscriptions.subscription_transactions CASCADE;
DROP TABLE IF EXISTS subscriptions.payment_methods CASCADE;
DROP TABLE IF EXISTS subscriptions.user_subscriptions CASCADE;
DROP TABLE IF EXISTS subscriptions.subscription_plans CASCADE;

-- Rollback V51 (schema)
DROP TYPE IF EXISTS subscriptions.payment_method_type CASCADE;
DROP TYPE IF EXISTS subscriptions.transaction_status CASCADE;
DROP TYPE IF EXISTS subscriptions.transaction_type CASCADE;
DROP TYPE IF EXISTS subscriptions.subscription_status CASCADE;
DROP SCHEMA IF EXISTS subscriptions CASCADE;
```

### Backward Compatibility

- `users.active_plan` column is kept in sync via trigger
- Existing code reading `users.active_plan` will continue to work
- Migrate application code gradually to use new schema

---

## Next Steps

### 1. Backend Implementation (Java)

**Domain Layer** (`subscriptions/domain/`):
```java
// domain/model/SubscriptionPlan.java
public class SubscriptionPlan {
    private Long id;
    private PlanCode planCode;
    private BigDecimal monthlyPrice;
    private BigDecimal commissionRate;
    // ... methods
}

// domain/model/UserSubscription.java
public class UserSubscription {
    private Long id;
    private Long userId;
    private SubscriptionPlan plan;
    private LocalDateTime expiresAt;
    private SubscriptionStatus status;
    // ... methods
}
```

**Ports** (`subscriptions/domain/port/`):
```java
public interface SubscriptionRepositoryPort {
    UserSubscription findByUserId(Long userId);
    UserSubscription save(UserSubscription subscription);
}

public interface PaymentGatewayPort {
    PaymentResult charge(PaymentMethod method, BigDecimal amount);
}
```

**Use Cases** (`subscriptions/application/usecase/`):
```java
@Service
public class UpgradeSubscriptionUseCase {
    public void execute(Long userId, PlanCode newPlan) {
        // 1. Get current subscription
        // 2. Validate upgrade
        // 3. Charge payment method
        // 4. Update subscription
        // 5. Record transaction
    }
}
```

### 2. Frontend Implementation (Next.js)

**Feature Structure** (`GydiFront/src/features/subscriptions/`):
```
subscriptions/
├── components/
│   ├── PricingCard.tsx
│   ├── PaymentMethodForm.tsx
│   └── SubscriptionDashboard.tsx
├── hooks/
│   ├── useSubscription.ts
│   ├── usePaymentMethods.ts
│   └── useUpgrade.ts
├── api/
│   └── subscriptionsApi.ts
├── schemas/
│   └── subscriptionSchemas.ts
└── types/
    └── subscription.types.ts
```

### 3. API Endpoints

**REST API** (`/api/subscriptions`):
```
GET    /api/subscriptions/plans               # List all plans
GET    /api/subscriptions/current             # Get user's current plan
POST   /api/subscriptions/upgrade             # Upgrade plan
POST   /api/subscriptions/downgrade           # Downgrade plan
POST   /api/subscriptions/cancel              # Cancel subscription
GET    /api/subscriptions/transactions        # Transaction history

POST   /api/payment-methods                   # Add payment method
GET    /api/payment-methods                   # List payment methods
PUT    /api/payment-methods/:id/default       # Set as default
DELETE /api/payment-methods/:id               # Delete (soft delete)
```

---

## Appendix

### A. Database Schema SQL Export

```bash
# Export schema only (no data)
pg_dump -h localhost -U postgres -d gydi_db \
    --schema=subscriptions \
    --schema-only \
    -f subscriptions_schema_export.sql

# Export with data
pg_dump -h localhost -U postgres -d gydi_db \
    --schema=subscriptions \
    -f subscriptions_full_export.sql
```

### B. Performance Benchmarks

**Query Performance Targets**:
- Get user plan: < 5ms
- List transactions: < 10ms
- Find expiring subscriptions: < 50ms

**Load Testing** (k6):
```javascript
import http from 'k6/http';

export default function() {
    http.get('http://localhost:8080/api/subscriptions/current');
}
```

### C. Monitoring & Alerts

**Metrics to Track**:
- Subscription status distribution (ACTIVE, EXPIRED, CANCELED)
- Failed payment rate (FAILED transactions / total transactions)
- Auto-renewal success rate
- Average transaction processing time

**Alerts**:
- ⚠️ High payment failure rate (> 5%)
- ⚠️ Subscriptions expiring without renewal attempts
- ⚠️ Database query latency > 100ms

---

## Conclusion

This schema design provides a robust, scalable foundation for the GYDI 2.0 subscription system. Key features:

✅ **Complete Audit Trail**: All transactions are immutably logged
✅ **PCI Compliance**: Tokenized payment methods only
✅ **Performance**: Comprehensive indexing strategy
✅ **Flexibility**: Supports upgrades, downgrades, renewals, cancellations
✅ **Backward Compatibility**: Syncs with existing `users.active_plan` column
✅ **Data Integrity**: Extensive constraints and business rules enforced at DB level

**Ready for implementation!** 🚀

---

**Document Version**: 1.0
**Last Updated**: 2025-12-09
**Author**: Database Architect AI
**Status**: Approved for Production
