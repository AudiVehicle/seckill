-- 生成的订单数据存入redis hash表结构中，key="bill"_${productId}，hkey=${userId},value=0 or 1 （表示是否已持久化到db）

local resultFlag = -1
local billKey = KEYS[1]
local userId = KEYS[2]
local productKey = KEYS[3]
local exist = redis.call("HGET", billKey, userId);
if exist then
    -- 已经购买过商品，直接return 0
    return -1;
end

local rest = tonumber(redis.call("GET", productKey))
if not rest then
    return -2
end
if rest > 0 then
    local ret = redis.call("DECR", productKey)
    redis.call("HSET", billKey, userId, 0);
    return ret
end
return -3