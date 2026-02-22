-- [Data Reset] Nameless Mall Stock Reset
-- Author: gem
-- Date: 2026-02-11
-- Purpose: Reset all normal product variant stock to 10,000,000 to resolve stock issues and ensure clean state for testing.

UPDATE mall_variant SET stock = 10000000;

-- Verify
SELECT count(*) as updated_count FROM mall_variant WHERE stock = 10000000;
