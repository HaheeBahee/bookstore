local itemCount = tonumber(ARGV[1])
local requestKey = KEYS[itemCount + 1]
local pendingKey = KEYS[itemCount + 2]

if redis.call('EXISTS', requestKey) == 1 then
    return -2
end

for i = 1, itemCount do
    local stock = redis.call('GET', KEYS[i])
    local quantity = tonumber(ARGV[6 + (i - 1) * 2])

    if not stock or tonumber(stock) < quantity then
        return -1
    end
end

redis.call('HSET', requestKey, 'status', 'PENDING', 'startedAt', ARGV[2])
redis.call('ZADD', pendingKey, ARGV[3], ARGV[4])

for i = 1, itemCount do
    local saleId = ARGV[5 + (i - 1) * 2]
    local quantity = tonumber(ARGV[6 + (i - 1) * 2])

    redis.call('DECRBY', KEYS[i], quantity)
    redis.call('HSET', requestKey, 'sale:' .. saleId, quantity)
end

return 1
