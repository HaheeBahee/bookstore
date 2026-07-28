package com.limitedmarket.api.global.redis;

import com.limitedmarket.api.domain.sale.RedisStockService;
import com.limitedmarket.api.domain.sale.Sale;
import com.limitedmarket.api.domain.sale.SaleRepository;
import com.limitedmarket.api.domain.sale.SaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisStockInitializer implements ApplicationRunner {

    private final SaleRepository saleRepository;
    private final RedisStockService redisStockService;

    @Override
    public void run(ApplicationArguments args) {
        List<Sale> activeSales = saleRepository.findBySaleStatusIn(
                List.of(SaleStatus.UPCOMING, SaleStatus.VIP_OPEN, SaleStatus.ON_SALE)
        );

        for (Sale sale : activeSales) {
            redisStockService.initStockIfAbsent(sale.getId(), sale.getRemainQuantity());
        }
    }
}
