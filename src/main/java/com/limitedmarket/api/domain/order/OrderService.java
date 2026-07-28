package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.domain.delivery.Delivery;
import com.limitedmarket.api.domain.delivery.DeliveryRepository;
import com.limitedmarket.api.domain.member.Member;
import com.limitedmarket.api.domain.member.MemberRepository;
import com.limitedmarket.api.domain.order.dto.*;
import com.limitedmarket.api.domain.payment.Payment;
import com.limitedmarket.api.domain.payment.PaymentRepository;
import com.limitedmarket.api.domain.payment.PaymentStatus;
import com.limitedmarket.api.domain.sale.RedisStockService;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final MemberRepository memberRepository;
    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;
    private final RedisStockService redisStockService;
    private final OrderCreateService orderCreateService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderCreateResponse create(OrderCreateRequest request, Long memberId, String requestId) {

        validateRequestId(requestId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<Order> existingOrder = orderRepository.findByRequestIdAndMemberId(requestId, memberId);
        if (existingOrder.isPresent()) {
            redisStockService.completeReservation(requestId);
            return OrderCreateResponse.from(existingOrder.get());
        }

        List<Long> saleIds = request.items().stream()
                .map(OrderItemRequest::saleId)
                .toList();

        Set<Long> uniqueIds = new HashSet<>(saleIds);
        if (uniqueIds.size() != saleIds.size()) {
            throw new CustomException(ErrorCode.DUPLICATE_SALE_ITEM);
        }

        Map<Long, Integer> quantityBySaleId = request.items().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::saleId,
                        OrderItemRequest::quantity
                ));

        // Redis에서 모든 재고 차감과 PENDING 기록을 원자적으로 처리
        redisStockService.reserve(requestId, quantityBySaleId);

        OrderCreateResponse response;
        try {
            response = orderCreateService.create(member, saleIds, quantityBySaleId, requestId);
        } catch (Exception e) {
            redisStockService.rollbackReservation(requestId, quantityBySaleId);
            throw e;
        }

        // OrderCreateService의 DB 트랜잭션 커밋이 끝난 뒤 COMPLETED로 변경
        redisStockService.completeReservation(requestId);
        return response;
    }

    private void validateRequestId(String requestId) {
        try {
            UUID.fromString(requestId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        return OrderDetailResponse.from(order, orderItems);
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getOrders(Long memberId) {
        return orderRepository.findByMemberId(memberId).stream()
                .map(OrderListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(Long orderId, Long memberId) {
        // 본인 주문인지 확인
        orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(OrderStatusHistoryResponse::from)
                .toList();
    }

    @Transactional
    public void cancel(Long orderId, Long memberId) {

        // 동시 취소 요청 방지 - 비관적 락으로 단일 트랜잭션만 처리
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (order.getOrderStatus() == OrderStatus.PAID) {
            // PAID 상태에서는 배송, 결제 정보가 반드시 존재해야 함
            // 없다면 데이터 정합성 오류
            Delivery delivery = deliveryRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
            if (!delivery.isCancellable()) {
                throw new CustomException(ErrorCode.ORDER_CANCEL_FAILED);
            }
            delivery.cancel();

            Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(orderId, PaymentStatus.PAID)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
            payment.refund();
        }

        order.cancel();
        orderStatusHistoryRepository.save(OrderStatusHistory.create(order, OrderStatus.CANCELLED, null));

        Map<Long, Integer> quantityBySaleId = new HashMap<>();
        List<OrderItem> orderItems = orderItemRepository.findByOrderWithSale(order);
        for (OrderItem orderItem : orderItems) {
            orderItem.getSale().restoreStock(orderItem.getQuantity(), LocalDateTime.now());
            quantityBySaleId.put(orderItem.getSale().getId(), orderItem.getQuantity());
        }
        eventPublisher.publishEvent(new OrderCancelledEvent(new ArrayList<>(quantityBySaleId.keySet()), quantityBySaleId));
    }
}
