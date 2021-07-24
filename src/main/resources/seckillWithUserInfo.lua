-- 生成的订单数据存入redis hash表结构中，billKey="bill"_${productId}，hkey=${userId}，hvalue=0 or 1 （1表示已持久化到db）
-- 返回不同的值，可以判断程序代码执行的分支逻辑

local billKey = KEYS[1]
local userId = KEYS[2]
local productKey = KEYS[3]
local exist = redis.call("HGET", billKey, userId);
if exist then
    -- 已经购买过商品，直接return -1
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