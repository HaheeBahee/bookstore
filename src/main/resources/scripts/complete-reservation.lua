local requestKey = KEYS[1]
local pendingKey = KEYS[2]
local status = redis.call('HGET', requestKey, 'status')

if status == 'PENDING' then
    redis.call('HSET', requestKey, 'status', 'COMPLETED', 'completedAt', ARGV[1])
    redis.call('PEXPIRE', requestKey, ARGV[2])
elseif status ~= 'COMPLETED' then
    redis.call('ZREM', pendingKey, ARGV[3])
    return 0
end

redis.call('ZREM', pendingKey, ARGV[3])

return 1
