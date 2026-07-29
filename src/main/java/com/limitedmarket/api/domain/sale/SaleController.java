package com.limitedmarket.api.domain.sale;

import com.limitedmarket.api.domain.sale.dto.SaleCreateRequest;
import com.limitedmarket.api.domain.sale.dto.SaleCreateResponse;
import com.limitedmarket.api.domain.sale.dto.SaleListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 판매")
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @Operation(summary = "판매 등록", description = "ADMIN 권한으로 상품 ID, 판매 가격, 재고 수량, VIP/일반 오픈 시간을 설정합니다")
    @ApiResponse(responseCode = "201", description = "판매 등록 성공")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SaleCreateResponse> create(@RequestBody @Valid SaleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(request));
    }

    @GetMapping
    @Operation(summary = "판매 중인 상품 목록 조회", description = "현재 VIP_OPEN 또는 ON_SALE 상태인 상품만 조회됩니다")
    public ResponseEntity<List<SaleListResponse>> getSales() {
        return ResponseEntity.ok(saleService.getSales());
    }


}
