-- ============================================================================
-- SUBSCRIPTION SYSTEM - USEFUL QUERIES
-- ============================================================================
-- Purpose: Collection of common queries for subscription management
-- Date: 2025-12-09
-- Usage: Reference for development, debugging, and reporting
-- ============================================================================

-- ============================================================================
-- 1. SUBSCRIPTION PLANS
-- ============================================================================

-- Get all active plans (for pricing page)
SELECT
    id,
    plan_code,
    plan_name,
    plan_description,
    monthly_price,
    commission_rate,
    referral_limit_per_month,
    property_publish_limit,
    is_featured,
    display_order
FROM subscriptions.subscription_plans
WHERE is_active = TRUE
ORDER BY display_order;

-- Get plan details by code
SELECT * FROM subscriptions.subscription_plans
WHERE plan_code = 'PRO';

-- Get commission rate for a plan
SELECT commission_rate
FROM subscriptions.subscription_plans
WHERE plan_code = 'ELITE';

-- ============================================================================
-- 2. USER SUBSCRIPTIONS
-- ============================================================================

-- Get user's current subscription with plan details
SELECT
    us.id AS subscription_id,
    u.id AS user_id,
    u.email,
    sp.plan_code,
    sp.plan_name,
    sp.monthly_price,
    sp.commission_rate,
    sp.referral_limit_per_month,
    sp.property_publish_limit,
    us.started_at,
    us.expires_at,
    us.status,
    us.auto_renew,
    CASE
        WHEN us.expires_at IS NULL THEN 'Never'
        WHEN us.expires_at < CURRENT_TIMESTAMP THEN 'Expired'
        ELSE CONCAT(EXTRACT(DAY FROM (us.expires_at - CURRENT_TIMESTAMP))::INT, ' days')
    END AS days_remaining
FROM subscriptions.user_subscriptions us
JOIN users.users u ON us.user_id = u.id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE u.id = 123;  -- Replace with actual user_id

-- Get all active subscriptions
SELECT
    sp.plan_code,
    COUNT(*) AS user_count,
    ROUND(AVG(sp.monthly_price), 2) AS avg_price,
    SUM(sp.monthly_price) AS total_mrr
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE'
GROUP BY sp.plan_code
ORDER BY user_count DESC;

-- Find subscriptions expiring in next 7 days (for renewal processing)
SELECT
    us.id AS subscription_id,
    u.id AS user_id,
    u.email,
    sp.plan_code,
    sp.monthly_price,
    us.expires_at,
    EXTRACT(DAY FROM (us.expires_at - CURRENT_TIMESTAMP))::INT AS days_until_expiry,
    us.auto_renew,
    pm.id AS payment_method_id,
    pm.card_last_four,
    pm.card_brand
FROM subscriptions.user_subscriptions us
JOIN users.users u ON us.user_id = u.id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
LEFT JOIN subscriptions.payment_methods pm ON us.payment_method_id = pm.id
WHERE us.status = 'ACTIVE'
  AND us.expires_at IS NOT NULL
  AND us.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days'
ORDER BY us.expires_at;

-- Find expired subscriptions (need to downgrade to FREE)
SELECT
    us.id AS subscription_id,
    u.id AS user_id,
    u.email,
    sp.plan_code,
    us.expires_at,
    us.status
FROM subscriptions.user_subscriptions us
JOIN users.users u ON us.user_id = u.id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE'
  AND us.expires_at IS NOT NULL
  AND us.expires_at < CURRENT_TIMESTAMP;

-- Find users with canceled subscriptions (not yet downgraded)
SELECT
    us.id AS subscription_id,
    u.id AS user_id,
    u.email,
    sp.plan_code,
    us.canceled_at,
    us.cancellation_reason,
    us.expires_at
FROM subscriptions.user_subscriptions us
JOIN users.users u ON us.user_id = u.id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'CANCELED'
ORDER BY us.canceled_at DESC;

-- ============================================================================
-- 3. PAYMENT METHODS
-- ============================================================================

-- Get user's payment methods
SELECT
    id,
    method_type,
    card_last_four,
    card_brand,
    card_expiry_month,
    card_expiry_year,
    paypal_email,
    is_default,
    is_active,
    created_at
FROM subscriptions.payment_methods
WHERE user_id = 123  -- Replace with actual user_id
  AND deleted_at IS NULL
ORDER BY is_default DESC, created_at DESC;

-- Get default payment method for user
SELECT
    id,
    method_type,
    gateway_provider,
    gateway_token,
    card_last_four,
    card_brand
FROM subscriptions.payment_methods
WHERE user_id = 123  -- Replace with actual user_id
  AND is_default = TRUE
  AND deleted_at IS NULL;

-- Find cards expiring in next 30 days (send reminder emails)
SELECT
    pm.id,
    u.id AS user_id,
    u.email,
    pm.card_last_four,
    pm.card_brand,
    pm.card_expiry_month,
    pm.card_expiry_year,
    CONCAT(pm.card_expiry_month, '/', pm.card_expiry_year) AS expiry_date
FROM subscriptions.payment_methods pm
JOIN users.users u ON pm.user_id = u.id
WHERE pm.deleted_at IS NULL
  AND pm.method_type IN ('CREDIT_CARD', 'DEBIT_CARD')
  AND pm.is_active = TRUE
  AND (
      (pm.card_expiry_year = EXTRACT(YEAR FROM CURRENT_TIMESTAMP)::INT
       AND pm.card_expiry_month <= EXTRACT(MONTH FROM CURRENT_TIMESTAMP)::INT + 1)
      OR
      (pm.card_expiry_year < EXTRACT(YEAR FROM CURRENT_TIMESTAMP)::INT)
  )
ORDER BY pm.card_expiry_year, pm.card_expiry_month;

-- ============================================================================
-- 4. SUBSCRIPTION TRANSACTIONS
-- ============================================================================

-- Get user's transaction history
SELECT
    st.id,
    from_plan.plan_code AS from_plan,
    to_plan.plan_code AS to_plan,
    st.transaction_type,
    st.transaction_status,
    st.amount,
    st.currency,
    st.gateway_provider,
    st.gateway_transaction_id,
    st.created_at,
    st.completed_at,
    st.failure_reason
FROM subscriptions.subscription_transactions st
LEFT JOIN subscriptions.subscription_plans from_plan ON st.from_plan_id = from_plan.id
JOIN subscriptions.subscription_plans to_plan ON st.to_plan_id = to_plan.id
WHERE st.user_id = 123  -- Replace with actual user_id
ORDER BY st.created_at DESC;

-- Get all pending transactions (need to be processed)
SELECT
    st.id,
    u.email,
    to_plan.plan_code AS new_plan,
    st.amount,
    st.transaction_type,
    st.retry_count,
    st.created_at
FROM subscriptions.subscription_transactions st
JOIN users.users u ON st.user_id = u.id
JOIN subscriptions.subscription_plans to_plan ON st.to_plan_id = to_plan.id
WHERE st.transaction_status = 'PENDING'
ORDER BY st.created_at;

-- Get failed transactions (for retry or investigation)
SELECT
    st.id,
    u.email,
    to_plan.plan_code AS target_plan,
    st.amount,
    st.transaction_type,
    st.failure_reason,
    st.retry_count,
    st.created_at
FROM subscriptions.subscription_transactions st
JOIN users.users u ON st.user_id = u.id
JOIN subscriptions.subscription_plans to_plan ON st.to_plan_id = to_plan.id
WHERE st.transaction_status = 'FAILED'
ORDER BY st.created_at DESC;

-- Revenue report by transaction type (last 30 days)
SELECT
    st.transaction_type,
    COUNT(*) AS transaction_count,
    SUM(st.amount) AS total_revenue,
    AVG(st.amount) AS avg_transaction_value
FROM subscriptions.subscription_transactions st
WHERE st.transaction_status = 'COMPLETED'
  AND st.created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY st.transaction_type
ORDER BY total_revenue DESC;

-- Monthly Recurring Revenue (MRR) breakdown
SELECT
    sp.plan_code,
    COUNT(us.id) AS active_subscriptions,
    sp.monthly_price,
    COUNT(us.id) * sp.monthly_price AS plan_mrr,
    ROUND((COUNT(us.id) * 100.0 / SUM(COUNT(us.id)) OVER ()), 2) AS percentage
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE'
GROUP BY sp.plan_code, sp.monthly_price, sp.display_order
ORDER BY sp.display_order;

-- Total MRR
SELECT
    COUNT(*) AS total_active_subscriptions,
    SUM(sp.monthly_price) AS total_mrr,
    AVG(sp.monthly_price) AS average_revenue_per_user
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE';

-- ============================================================================
-- 5. ANALYTICS & REPORTING
-- ============================================================================

-- Subscription conversion funnel (FREE → PRO → ELITE)
WITH user_journey AS (
    SELECT
        st.user_id,
        ARRAY_AGG(to_plan.plan_code ORDER BY st.created_at) AS plan_sequence
    FROM subscriptions.subscription_transactions st
    JOIN subscriptions.subscription_plans to_plan ON st.to_plan_id = to_plan.id
    WHERE st.transaction_status = 'COMPLETED'
    GROUP BY st.user_id
)
SELECT
    plan_sequence[1] AS initial_plan,
    CASE
        WHEN ARRAY_LENGTH(plan_sequence, 1) >= 2 THEN plan_sequence[2]
        ELSE 'No upgrade'
    END AS second_plan,
    CASE
        WHEN ARRAY_LENGTH(plan_sequence, 1) >= 3 THEN plan_sequence[3]
        ELSE 'No further upgrade'
    END AS third_plan,
    COUNT(*) AS user_count
FROM user_journey
GROUP BY plan_sequence[1], plan_sequence[2], plan_sequence[3]
ORDER BY user_count DESC;

-- Churn analysis (users who canceled)
SELECT
    sp.plan_code,
    COUNT(*) AS cancellations,
    AVG(EXTRACT(DAY FROM (us.canceled_at - us.started_at))) AS avg_days_before_cancel,
    STRING_AGG(DISTINCT us.cancellation_reason, '; ') AS common_reasons
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'CANCELED'
  AND us.canceled_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY sp.plan_code;

-- Upgrade rate (FREE → Paid)
WITH plan_changes AS (
    SELECT
        user_id,
        MIN(CASE WHEN to_plan.plan_code != 'FREE' THEN st.created_at END) AS first_paid_date
    FROM subscriptions.subscription_transactions st
    JOIN subscriptions.subscription_plans to_plan ON st.to_plan_id = to_plan.id
    WHERE st.transaction_status = 'COMPLETED'
    GROUP BY user_id
)
SELECT
    COUNT(DISTINCT u.id) AS total_users,
    COUNT(DISTINCT pc.user_id) AS users_upgraded_to_paid,
    ROUND(COUNT(DISTINCT pc.user_id) * 100.0 / COUNT(DISTINCT u.id), 2) AS upgrade_rate_percent
FROM users.users u
LEFT JOIN plan_changes pc ON u.id = pc.user_id;

-- Lifetime Value (LTV) by plan
SELECT
    sp.plan_code,
    COUNT(DISTINCT st.user_id) AS unique_users,
    SUM(st.amount) AS total_revenue,
    AVG(st.amount) AS avg_transaction,
    SUM(st.amount) / COUNT(DISTINCT st.user_id) AS ltv_per_user
FROM subscriptions.subscription_transactions st
JOIN subscriptions.subscription_plans sp ON st.to_plan_id = sp.id
WHERE st.transaction_status = 'COMPLETED'
GROUP BY sp.plan_code
ORDER BY ltv_per_user DESC;

-- Revenue trend (last 12 months)
SELECT
    DATE_TRUNC('month', st.created_at) AS month,
    COUNT(*) AS transaction_count,
    SUM(st.amount) AS revenue,
    COUNT(DISTINCT st.user_id) AS unique_users
FROM subscriptions.subscription_transactions st
WHERE st.transaction_status = 'COMPLETED'
  AND st.created_at >= CURRENT_TIMESTAMP - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', st.created_at)
ORDER BY month DESC;

-- ============================================================================
-- 6. HELPER FUNCTIONS
-- ============================================================================

-- Check if subscription is expired
SELECT subscriptions.is_subscription_expired(123);  -- Replace with subscription_id

-- Auto-expire subscriptions (run daily)
SELECT * FROM subscriptions.auto_expire_subscriptions();

-- Get user plan details (using helper function)
SELECT * FROM subscriptions.get_user_plan(123);  -- Replace with user_id

-- ============================================================================
-- 7. ADMIN OPERATIONS
-- ============================================================================

-- Manually upgrade user to PRO
BEGIN;

UPDATE subscriptions.user_subscriptions
SET
    plan_id = (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'PRO'),
    expires_at = CURRENT_TIMESTAMP + INTERVAL '30 days',
    auto_renew = FALSE,  -- Manual upgrade, no auto-renew
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 123;  -- Replace with user_id

INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    transaction_type,
    transaction_status,
    amount,
    period_start,
    period_end,
    completed_at,
    metadata
) VALUES (
    (SELECT id FROM subscriptions.user_subscriptions WHERE user_id = 123),
    123,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE'),
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'PRO'),
    'UPGRADE',
    'COMPLETED',
    0.00,  -- Manual upgrade, no charge
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP,
    '{"note": "Manual admin upgrade", "admin_id": 1}'::JSONB
);

COMMIT;

-- Manually cancel user subscription
BEGIN;

UPDATE subscriptions.user_subscriptions
SET
    status = 'CANCELED',
    auto_renew = FALSE,
    canceled_at = CURRENT_TIMESTAMP,
    cancellation_reason = 'Admin action: account suspended',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 123;  -- Replace with user_id

INSERT INTO subscriptions.subscription_transactions (
    user_subscription_id,
    user_id,
    from_plan_id,
    to_plan_id,
    transaction_type,
    transaction_status,
    amount,
    completed_at,
    metadata
) VALUES (
    (SELECT id FROM subscriptions.user_subscriptions WHERE user_id = 123),
    123,
    (SELECT us.plan_id FROM subscriptions.user_subscriptions us WHERE us.user_id = 123),
    (SELECT us.plan_id FROM subscriptions.user_subscriptions us WHERE us.user_id = 123),
    'CANCELLATION',
    'COMPLETED',
    0.00,
    CURRENT_TIMESTAMP,
    '{"note": "Manual admin cancellation", "admin_id": 1}'::JSONB
);

COMMIT;

-- ============================================================================
-- 8. MAINTENANCE QUERIES
-- ============================================================================

-- Check for data integrity issues

-- Users without subscriptions (should be 0)
SELECT u.id, u.email
FROM users.users u
LEFT JOIN subscriptions.user_subscriptions us ON u.id = us.user_id
WHERE us.id IS NULL;

-- Subscriptions with invalid plan_id
SELECT us.id, us.user_id
FROM subscriptions.user_subscriptions us
LEFT JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE sp.id IS NULL;

-- Users with multiple active subscriptions (should be 0 - violates business rule)
SELECT user_id, COUNT(*) AS subscription_count
FROM subscriptions.user_subscriptions
WHERE status = 'ACTIVE'
GROUP BY user_id
HAVING COUNT(*) > 1;

-- Payment methods with invalid tokens (example validation)
SELECT id, user_id, gateway_provider, LENGTH(gateway_token) AS token_length
FROM subscriptions.payment_methods
WHERE deleted_at IS NULL
  AND (gateway_token IS NULL OR gateway_token = '');

-- Users.active_plan out of sync with subscriptions
SELECT
    u.id,
    u.email,
    u.active_plan AS old_plan,
    sp.plan_code AS current_plan
FROM users.users u
JOIN subscriptions.user_subscriptions us ON u.id = us.user_id
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE us.status = 'ACTIVE'
  AND u.active_plan != sp.plan_code;

-- ============================================================================
-- 9. PERFORMANCE MONITORING
-- ============================================================================

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'subscriptions'
ORDER BY idx_scan DESC;

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'subscriptions'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Active connections to subscriptions schema
SELECT
    datname,
    usename,
    application_name,
    client_addr,
    state,
    query
FROM pg_stat_activity
WHERE query LIKE '%subscriptions%'
  AND state = 'active';

-- ============================================================================
-- 10. VACUUM & ANALYZE (Maintenance)
-- ============================================================================

-- Vacuum and analyze all subscriptions tables
VACUUM ANALYZE subscriptions.subscription_plans;
VACUUM ANALYZE subscriptions.user_subscriptions;
VACUUM ANALYZE subscriptions.payment_methods;
VACUUM ANALYZE subscriptions.subscription_transactions;

-- Rebuild indexes (if fragmented)
REINDEX TABLE subscriptions.user_subscriptions;
REINDEX TABLE subscriptions.subscription_transactions;

-- ============================================================================
-- END OF QUERIES
-- ============================================================================
