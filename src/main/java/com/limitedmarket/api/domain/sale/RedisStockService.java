package com.limitedmarket.api.domain.sale;

import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisStockService {

    private static final String PENDING_REQUESTS_KEY = "pending:requests";
    private static final long PENDING_TIMEOUT_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final long TERMINAL_REQUEST_TTL_MILLIS = Duration.ofHours(24).toMillis();

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> decreaseStockScript;
    private final RedisScript<Long> rollbackReservationScript;
    private final RedisScript<Long> completeReservationScript;

    public RedisStockService(
            StringRedisTemplate redisTemplate,
            @Qualifier("decreaseStockScript") RedisScript<Long> decreaseStockScript,
            @Qualifier("rollbackReservationScript") RedisScript<Long> rollbackReservationScript,
            @Qualifier("completeReservationScript") RedisScript<Long> completeReservationScript
    ) {
        this.redisTemplate = redisTemplate;
        this.decreaseStockScript = decreaseStockScript;
        this.rollbackReservationScript = rollbackReservationScript;
        this.completeReservationScript = completeReservationScript;
    }

    // Redis 재고 키가 없을 때만 DB 재고로 초기화
    public void initStockIfAbsent(Long saleId, int quantity) {
        redisTemplate.opsForValue().setIfAbsent(
                "sale:stock:" + saleId,
                String.valueOf(quantity)
        );
    }

    // 주문에 포함된 모든 재고 차감과 PENDING 기록을 원자적으로 처리
    public void reserve(String requestId, Map<Long, Integer> quantityBySaleId) {
        List<Map.Entry<Long, Integer>> items = sortedItems(quantityBySaleId);
        List<String> keys = createKeys(requestId, items);
        long startedAt = Instant.now().toEpochMilli();
        List<String> args = createArgs(
                requestId,
                items,
                startedAt,
                startedAt + PENDING_TIMEOUT_MILLIS
        );

        Long result = redisTemplate.execute(
                decreaseStockScript,
                keys,
                args.toArray()
        );

        if (result != null && result == -2L) {
            throw new CustomException(ErrorCode.ORDER_REQUEST_IN_PROGRESS);
        }
        if (result == null || result < 0) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }
    }

    // DB 처리 실패 시 재고 복구와 FAILED 변경을 원자적으로 처리
    public void rollbackReservation(String requestId, Map<Long, Integer> quantityBySaleId) {
        List<Map.Entry<Long, Integer>> items = sortedItems(quantityBySaleId);
        List<String> keys = createKeys(requestId, items);
        List<String> args = createArgs(
                requestId,
                items,
                Instant.now().toEpochMilli(),
                TERMINAL_REQUEST_TTL_MILLIS
        );

        redisTemplate.execute(
                rollbackReservationScript,
                keys,
                args.toArray()
        );
    }

    // DB 커밋 성공 후 PENDING을 COMPLETED로 변경
    public void completeReservation(String requestId) {
        redisTemplate.execute(
                completeReservationScript,
                List.of("request:" + requestId, PENDING_REQUESTS_KEY),
                String.valueOf(Instant.now().toEpochMilli()),
                String.valueOf(TERMINAL_REQUEST_TTL_MILLIS),
                requestId
        );
    }

    public Set<String> getExpiredPendingRequestIds() {
        Set<String> requestIds = redisTemplate.opsForZSet()
                .rangeByScore(PENDING_REQUESTS_KEY, 0, Instant.now().toEpochMilli());
        return requestIds != null ? requestIds : Set.of();
    }

    public Map<Long, Integer> getReservedQuantities(String requestId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("request:" + requestId);
        Map<Long, Integer> quantities = new HashMap<>();

        entries.forEach((field, value) -> {
            String fieldName = String.valueOf(field);
            if (fieldName.startsWith("sale:")) {
                Long saleId = Long.valueOf(fieldName.substring("sale:".length()));
                quantities.put(saleId, Integer.valueOf(String.valueOf(value)));
            }
        });
        return quantities;
    }

    // 재고 복구 - 차감한 재고 복구
    // 언제 사용 - DB 트랜잭션 실패 시, 주문 취소시
    public void restore(Long saleId, int quantity) {
        redisTemplate.opsForValue().increment("sale:stock:" + saleId, quantity);
    }

    public int getStock(Long saleId) {
        String value = redisTemplate.opsForValue().get("sale:stock:" + saleId);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private List<Map.Entry<Long, Integer>> sortedItems(Map<Long, Integer> quantityBySaleId) {
        return quantityBySaleId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    private List<String> createKeys(String requestId, List<Map.Entry<Long, Integer>> items) {
        List<String> keys = new ArrayList<>();
        items.forEach(item -> keys.add("sale:stock:" + item.getKey()));
        keys.add("request:" + requestId);
        keys.add(PENDING_REQUESTS_KEY);
        return keys;
    }

    private List<String> createArgs(
            String requestId,
            List<Map.Entry<Long, Integer>> items,
            long timestamp,
            long thirdArgument
    ) {
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(items.size()));
        args.add(String.valueOf(timestamp));
        args.add(String.valueOf(thirdArgument));
        args.add(requestId);
        items.forEach(item -> {
            args.add(String.valueOf(item.getKey()));
            args.add(String.valueOf(item.getValue()));
        });
        return args;
    }
}
