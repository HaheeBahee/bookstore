local stock = redis.call('GET', KEYS[1])

if not stock then
    return -1
end

if tonumber(stock) < tonumber(ARGV[1]) then
    return -1
end

return redis.call('DECRBY', KEYS[1], ARGV[1])
