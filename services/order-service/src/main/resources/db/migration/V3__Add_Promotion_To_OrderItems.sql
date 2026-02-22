-- ============================================================================
-- V3: Add Promotion Snapshot Columns to Order Items
-- Purpose: Store the immutable snapshot of promotion details at the time of purchase.
-- Created By: AI Architect
-- Date: 2026-02-07
-- ============================================================================

-- 1. Add 'original_price' column (Decimal 10,2 for price)
ALTER TABLE order_items 
ADD COLUMN original_price DECIMAL(10,2) DEFAULT NULL COMMENT '商品原價 (交易快照)';

-- 2. Add 'promotion_name' column (Text for promotion description)
ALTER TABLE order_items 
ADD COLUMN promotion_name VARCHAR(255) DEFAULT NULL COMMENT '促銷活動名稱 (交易快照)';

-- 3. Add 'promotion_amount' column (Decimal 10,2 for discount value)
ALTER TABLE order_items 
ADD COLUMN promotion_amount DECIMAL(10,2) DEFAULT NULL COMMENT '促銷折扣金額 (交易快照)';

-- Verify changes (Optional)
-- DESC order_items;
