package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_history_order_id_created_at", columnList = "order_id, created_at")
})
public class OrderStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String reason;

    public static OrderStatusHistory create(Order order, OrderStatus status, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.status = status;
        history.reason = reason;
        return history;
    }
}
