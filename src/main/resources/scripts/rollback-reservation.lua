local itemCount = tonumber(ARGV[1])
local requestKey = KEYS[itemCount + 1]
local pendingKey = KEYS[itemCount + 2]

if redis.call('HGET', requestKey, 'status') ~= 'PENDING' then
    redis.call('ZREM', pendingKey, ARGV[4])
    return 0
end

for i = 1, itemCount do
    local quantity = tonumber(ARGV[6 + (i - 1) * 2])
    redis.call('INCRBY', KEYS[i], quantity)
end

redis.call('HSET', requestKey, 'status', 'FAILED', 'failedAt', ARGV[2])
redis.call('PEXPIRE', requestKey, ARGV[3])
redis.call('ZREM', pendingKey, ARGV[4])

return 1
