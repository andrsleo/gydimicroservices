-- V34__remove_name_from_users.sql
-- Update dependent views and remove name column from users table

-- 1. Update v_pending_payouts to use user_profile for names instead of users.name
CREATE OR REPLACE VIEW referrals.v_pending_payouts AS
SELECT
    cl.affiliate_id,
    u.email AS affiliate_email,
    COALESCE(
        NULLIF(TRIM(CONCAT_WS(' ', up.first_name, up.last_name)), ''),
        u.email
    ) AS affiliate_name,
    COUNT(cl.id) AS commission_count,
    SUM(cl.commission_amount) AS total_payout_amount,
    MIN(cl.approved_at) AS earliest_approval_date,
    MAX(cl.approved_at) AS latest_approval_date,
    ARRAY_AGG(cl.id ORDER BY cl.approved_at) AS commission_ids
FROM referrals.commission_ledger cl
JOIN users.users u ON u.id = cl.affiliate_id
LEFT JOIN users.user_profile up ON up.user_id = u.id
WHERE cl.status = 'APPROVED'
GROUP BY cl.affiliate_id, u.email, up.first_name, up.last_name
HAVING SUM(cl.commission_amount) >= 50.00
ORDER BY SUM(cl.commission_amount) DESC;

-- 2. Drop name column from users table (if it exists)
ALTER TABLE users.users DROP COLUMN IF EXISTS name;
