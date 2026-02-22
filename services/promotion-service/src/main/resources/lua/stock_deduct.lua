-- KEYS[1]: 庫存 Key (flash_sale:stock:{promoId}:{skuId})
-- ARGV[1]: 扣減數量
local stock = tonumber(redis.call('get', KEYS[1]) or '-1')

-- 1. 檢查 Key 是否存在（預熱檢查）
if stock == -1 then 
    return -2 
end 

-- 2. 庫存判定與扣減
local qty = tonumber(ARGV[1])
if stock >= qty then 
    return redis.call('decrby', KEYS[1], qty) 
else 
    return -1 -- 庫存不足
end
