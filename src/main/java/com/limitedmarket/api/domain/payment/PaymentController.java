package com.limitedmarket.api.domain.payment;

import com.limitedmarket.api.domain.payment.dto.PaymentRequest;
import com.limitedmarket.api.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@Tag(name = "5. 결제")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    @Operation(
            summary = "결제(Mock)",
            description = "실제 결제 연동 없이 결제 완료 처리합니다. impUid는 결제 요청마다 다른 값을 입력해야 합니다."
    )
    public ResponseEntity<Void> pay(@PathVariable Long orderId,
                                    @RequestBody @Valid PaymentRequest request,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.pay(orderId, userDetails.getMemberId(), request);
        return ResponseEntity.ok().build();
    }
}
