-- =====================================================
-- 異步下單架構遷移 SQL
-- 執行目標: mall_order 資料庫
-- 說明: 新增 fail_reason 欄位供異步下單失敗時記錄原因
-- 新增狀態碼: 5=建立中(CREATING), 6=建立失敗(CREATE_FAILED)
-- =====================================================

-- 1. orders 表新增 fail_reason 欄位
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fail_reason VARCHAR(512) DEFAULT NULL COMMENT '訂單建立失敗原因（異步下單用）' AFTER note;

-- 2. 為 CREATING 狀態的訂單加索引（卡單清理排程用）
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders (status, created_at);
