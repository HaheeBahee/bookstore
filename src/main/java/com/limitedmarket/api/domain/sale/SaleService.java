package com.limitedmarket.api.domain.sale;

import com.limitedmarket.api.domain.product.Product;
import com.limitedmarket.api.domain.product.ProductRepository;
import com.limitedmarket.api.domain.sale.dto.SaleCreateRequest;
import com.limitedmarket.api.domain.sale.dto.SaleCreateResponse;
import com.limitedmarket.api.domain.sale.dto.SaleListResponse;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final RedisStockService redisStockService;

    @Transactional
    public SaleCreateResponse create(SaleCreateRequest request) {

        // 날짜 순서 검증: vipOpenAt <= generalOpenAt < closeAt
        if (request.vipOpenAt().isAfter(request.generalOpenAt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!request.generalOpenAt().isBefore(request.closeAt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        Sale sale = Sale.create(product,
                request.salePrice(),
                request.totalQuantity(),
                request.vipOpenAt(),
                request.generalOpenAt(),
                request.closeAt());

        saleRepository.save(sale);
        redisStockService.initStockIfAbsent(sale.getId(), sale.getTotalQuantity());
        return SaleCreateResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleListResponse> getSales() {
        return saleRepository.findBySaleStatusIn(
                        List.of(SaleStatus.VIP_OPEN, SaleStatus.ON_SALE)
                ).stream()
                .map(sale -> SaleListResponse.from(sale, redisStockService.getStock(sale.getId())))
                .toList();
    }
}
