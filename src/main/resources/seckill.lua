local resultFlag = 0
local key = tonumber(KEYS[1])
local rest = tonumber(redis.call("GET", key))
if not rest then
    return resultFlag
end
--io.write(rest)
if rest > 0 then
    local ret = redis.call("DECR", key)
    return ret
end
return resultFlag

--local tb={t1=11,t2=22}
--for k,v in pairs(tb) do
--    redis.call("set",k,v)
--end