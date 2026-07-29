package com.limitedmarket.api.domain.payment;

import com.limitedmarket.api.domain.delivery.DeliveryService;
import com.limitedmarket.api.domain.order.*;
import com.limitedmarket.api.domain.payment.dto.PaymentRequest;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional
    public void pay(Long orderId, Long memberId, PaymentRequest request) {

        // 동시 결제 요청 방지 - 비관적 락으로 단일 처리
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        // Mock 결제에서는 요청 금액과 주문 금액 일치 여부만 검증한다.
        if (request.amount().compareTo(order.getTotalPrice()) != 0) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (paymentRepository.existsByImpUid(request.impUid())) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        order.markAsPaid();

        Payment payment = Payment.create(order, order.getTotalPrice(), request.impUid());
        paymentRepository.save(payment);
        orderStatusHistoryRepository.save(OrderStatusHistory.create(order, OrderStatus.PAID, null));

        deliveryService.create(order, request);
    }

}
