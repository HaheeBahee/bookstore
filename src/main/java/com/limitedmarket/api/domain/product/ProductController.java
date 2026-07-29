package com.limitedmarket.api.domain.product;

import com.limitedmarket.api.domain.product.dto.ProductCreateRequest;
import com.limitedmarket.api.domain.product.dto.ProductCreateResponse;
import com.limitedmarket.api.domain.product.dto.ProductDetailResponse;
import com.limitedmarket.api.domain.product.dto.ProductListResponse;
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
@Tag(name = "2. 상품")
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "상품 등록", description = "ADMIN 권한이 필요합니다")
    @ApiResponse(responseCode = "201", description = "상품 등록 성공")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductCreateResponse> register(@RequestBody @Valid ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.registerProduct(request));
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회")
    public ResponseEntity<List<ProductListResponse>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
}
