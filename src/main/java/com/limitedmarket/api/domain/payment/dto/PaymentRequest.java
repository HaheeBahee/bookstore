package com.limitedmarket.api.domain.payment.dto;

import com.limitedmarket.api.domain.delivery.dto.DeliveryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @Schema(
                description = "결제 요청 고유 ID입니다. 결제마다 다른 값을 입력해야 합니다.",
                example = "imp_mock_550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank String impUid,
        @Schema(example = "30000")
        @NotNull BigDecimal amount,
        @Valid @NotNull DeliveryRequest deliveryRequest
) {
}
