-- V5: Fix ip_address column type from INET to VARCHAR for Hibernate compatibility
ALTER TABLE transactions ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR;
