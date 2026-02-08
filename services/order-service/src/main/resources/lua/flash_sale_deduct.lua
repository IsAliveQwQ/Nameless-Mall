-- KEYS[1]: 庫存 Key (flash_sale:stock:{promotionId}:{skuId})
-- KEYS[2]: 用戶排隊 Key (flash_sale:order:{userId}:{skuId})
-- ARGV[1]: 扣減數量 (quantity)

local stockKey = KEYS[1]
local orderStatusKey = KEYS[2]
local quantity = tonumber(ARGV[1])

-- 1. 檢查是否重複排隊 (使用 setnx 原子性搶占)
-- 如果 Key 已存在，代表已經排隊或購買過，直接返回重複
if redis.call('exists', orderStatusKey) == 1 then
    return -3 -- 重複排隊
end

-- 2. 檢查庫存 Key 是否存在 (預熱檢查)
if redis.call('exists', stockKey) == 0 then
    return -2 -- 庫存未預熱
end

-- 3. 獲取並檢查庫存
local currentStock = tonumber(redis.call('get', stockKey))
if currentStock < quantity then
    return -1 -- 庫存不足
end

-- 4. 執行扣減與佔位 (原子操作)
redis.call('decrby', stockKey, quantity)
-- 預先設置為 PENDING，TTL 10分鐘防止死鎖
redis.call('setex', orderStatusKey, 600, "PENDING")

return 1 -- 成功
