package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.domain.sale.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRequestRecoveryScheduler {

    private final OrderRepository orderRepository;
    private final RedisStockService redisStockService;

    @Scheduled(fixedDelay = 60_000)
    public void recoverPendingRequests() {
        for (String requestId : redisStockService.getExpiredPendingRequestIds()) {
            recover(requestId);
        }
    }

    private void recover(String requestId) {
        try {
            if (orderRepository.existsByRequestId(requestId)) {
                redisStockService.completeReservation(requestId);
                return;
            }

            Map<Long, Integer> quantities =
                    redisStockService.getReservedQuantities(requestId);
            redisStockService.rollbackReservation(requestId, quantities);
            log.warn("[PendingRecovery] 미완료 주문 재고 복구 - requestId={}", requestId);
        } catch (Exception e) {
            log.error("[PendingRecovery] 복구 실패 - requestId={}", requestId, e);
        }
    }
}
