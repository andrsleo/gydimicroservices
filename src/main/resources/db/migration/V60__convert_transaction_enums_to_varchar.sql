-- V60__convert_transaction_enums_to_varchar.sql
-- Description: Convert transaction_type and transaction_status from PostgreSQL ENUMs to VARCHAR
-- Author: GYDI Development Team
-- Date: 2025-12-16
-- Reason: Hibernate has compatibility issues with PostgreSQL ENUM types
-- Note: Table is empty, so we can safely DROP and recreate the columns

-- Step 1: Drop the enum columns
ALTER TABLE subscriptions.subscription_transactions
DROP COLUMN transaction_type;

ALTER TABLE subscriptions.subscription_transactions
DROP COLUMN transaction_status;

-- Step 2: Recreate as VARCHAR with defaults and constraints
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN transaction_type VARCHAR(30) NOT NULL;

ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN transaction_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Step 3: Add check constraints to maintain data integrity
ALTER TABLE subscriptions.subscription_transactions
ADD CONSTRAINT chk_transaction_type CHECK (
    transaction_type IN (
        'INITIAL_SUBSCRIPTION',
        'UPGRADE',
        'RENEWAL',
        'DOWNGRADE',
        'CANCELLATION',
        'REACTIVATION'
    )
);

ALTER TABLE subscriptions.subscription_transactions
ADD CONSTRAINT chk_transaction_status CHECK (
    transaction_status IN (
        'PENDING',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'REFUNDED',
        'CANCELED'
    )
);

-- Step 4: Add comments
COMMENT ON COLUMN subscriptions.subscription_transactions.transaction_type IS
'Type of transaction (VARCHAR). Allowed values: INITIAL_SUBSCRIPTION, UPGRADE, RENEWAL, DOWNGRADE, CANCELLATION, REACTIVATION';

COMMENT ON COLUMN subscriptions.subscription_transactions.transaction_status IS
'Status of transaction (VARCHAR). Allowed values: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELED';
