package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.domain.order.dto.OrderCreateRequest;
import com.limitedmarket.api.domain.order.dto.OrderCreateResponse;
import com.limitedmarket.api.domain.order.dto.OrderDetailResponse;
import com.limitedmarket.api.domain.order.dto.OrderListResponse;
import com.limitedmarket.api.domain.order.dto.OrderStatusHistoryResponse;
import com.limitedmarket.api.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "4. 주문")
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "로그인 후 사용 가능합니다. 상단 Authorize 버튼에 토큰을 먼저 입력하세요")
    public ResponseEntity<OrderCreateResponse> create(@RequestHeader("Idempotency-Key") String requestId,
                                                      @RequestBody @Valid OrderCreateRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderCreateResponse response = orderService.create(request, userDetails.getMemberId(), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "내 주문 목록 조회", description = "로그인한 회원의 주문 목록을 조회합니다.")
    public ResponseEntity<List<OrderListResponse>> getOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(orderService.getOrders(userDetails.getMemberId()));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "내 주문 상세 조회")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable Long orderId,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(orderService.getOrder(orderId, userDetails.getMemberId()));
    }

    @GetMapping("/{orderId}/history")
    @Operation(summary = "주문 상태 변경 이력 조회", description = "PENDING → PAID → CANCELLED 등 상태 변경 시각을 확인할 수 있습니다")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(orderService.getOrderHistory(orderId, userDetails.getMemberId()));
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "PENDING 또는 PAID 상태의 주문만 취소 가능합니다")
    public ResponseEntity<Void> cancel(@PathVariable Long orderId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.cancel(orderId, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }
}
