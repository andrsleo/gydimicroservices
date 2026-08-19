-- Cleanup script for booking tests
-- Used by @Sql annotation in integration tests
-- Works with both H2 (test) and PostgreSQL (local/prod)

DELETE FROM booking WHERE id > 0;
