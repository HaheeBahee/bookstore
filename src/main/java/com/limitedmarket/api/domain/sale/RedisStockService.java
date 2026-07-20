package com.limitedmarket.api.domain.sale;

import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> decreaseStockScript;

    // 재고 초기화 - DB에 있는 재고 -> Redis
    public void initStock(Long saleId, int quantity) {
        redisTemplate.opsForValue().set("sale:stock:" + saleId, String.valueOf(quantity));
    }

    // 재고 차감 - 주문 요청에서 Redis에서 먼저 재고 차감
    public void decrease(Long saleId, int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String key = "sale:stock:" + saleId;

        Long remain = redisTemplate.execute(
                decreaseStockScript,
                List.of(key),
                String.valueOf(quantity)
            );

        if (remain == null || remain < 0) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }
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
}
