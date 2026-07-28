package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.domain.member.Member;
import com.limitedmarket.api.global.BaseEntity;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    public static Order create(
            Member member,
            BigDecimal totalPrice,
            String requestId
    ) {
        Order order = new Order();
        order.member = member;
        order.requestId = requestId;
        order.orderStatus = OrderStatus.PENDING;
        order.totalPrice = totalPrice;
        return order;
    }

    public void cancel() {
        if (this.orderStatus != OrderStatus.PENDING
                && this.orderStatus != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        this.orderStatus = OrderStatus.CANCELLED;
    }

    public void markAsPaid() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
        this.orderStatus = OrderStatus.PAID;
    }

}
