-- Migration to add implicit casts from VARCHAR to custom ENUM types
-- This allows Hibernate to send VARCHAR values that PostgreSQL will automatically convert to ENUMs

-- Cast for subscription_status
CREATE CAST (varchar AS subscriptions.subscription_status) 
WITH INOUT AS IMPLICIT;

-- Cast for transaction_type  
CREATE CAST (varchar AS subscriptions.transaction_type) 
WITH INOUT AS IMPLICIT;

-- Cast for transaction_status
CREATE CAST (varchar AS subscriptions.transaction_status) 
WITH INOUT AS IMPLICIT;
