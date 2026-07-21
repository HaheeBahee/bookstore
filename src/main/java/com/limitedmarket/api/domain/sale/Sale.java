package com.limitedmarket.api.domain.sale;

import com.limitedmarket.api.domain.member.Grade;
import com.limitedmarket.api.domain.product.Product;
import com.limitedmarket.api.global.BaseEntity;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sale", indexes = {
        @Index(name = "idx_sale_status_vip_open_at", columnList = "sale_status, vip_open_at"),
        @Index(name = "idx_sale_status_general_open_at", columnList = "sale_status, general_open_at"),
        @Index(name = "idx_sale_status_close_at", columnList = "sale_status, close_at")
})
public class Sale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int remainQuantity;

    @Column(nullable = false)
    private LocalDateTime vipOpenAt;

    @Column(nullable = false)
    private LocalDateTime generalOpenAt;

    @Column(nullable = false)
    private LocalDateTime closeAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus saleStatus = SaleStatus.UPCOMING; // UPCOMING, VIP_OPEN, ON_SALE, SOLD_OUT, CLOSED


    public static Sale create(
            Product product,
            BigDecimal salePrice,
            int totalQuantity,
            LocalDateTime vipOpenAt,
            LocalDateTime generalOpenAt,
            LocalDateTime closeAt
    ) {
        Sale sale = new Sale();
        sale.product = product;
        sale.salePrice = salePrice;
        sale.totalQuantity = totalQuantity;
        sale.remainQuantity = totalQuantity;
        sale.vipOpenAt = vipOpenAt;
        sale.generalOpenAt = generalOpenAt;
        sale.closeAt = closeAt;
        sale.saleStatus = SaleStatus.UPCOMING;

        return sale;
    }

    public void openForVip() {
        this.saleStatus = SaleStatus.VIP_OPEN;
    }

    public void openForGeneral() {
        this.saleStatus = SaleStatus.ON_SALE;
    }

    public void soldOut() {
        this.saleStatus = SaleStatus.SOLD_OUT;
    }

    public void close() {
        this.saleStatus = SaleStatus.CLOSED;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (this.remainQuantity < quantity) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }
        this.remainQuantity -= quantity;

        if (this.remainQuantity == 0) {
            this.saleStatus = SaleStatus.SOLD_OUT;
        }
    }

    public void restoreStock(int quantity, LocalDateTime now) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        this.remainQuantity += quantity;

        if (this.saleStatus == SaleStatus.SOLD_OUT && this.remainQuantity > 0) {
            if (now.isBefore(this.vipOpenAt)) {
                this.saleStatus = SaleStatus.UPCOMING;
            } else if (now.isBefore(this.generalOpenAt)) {
                this.saleStatus = SaleStatus.VIP_OPEN;
            } else if (now.isAfter(this.closeAt)) {
                this.saleStatus = SaleStatus.CLOSED;
            } else {
                this.saleStatus = SaleStatus.ON_SALE;
            }
        }
    }

    public LocalDateTime getOrderOpenAt(Grade grade) {
        if (grade == Grade.VIP) {
            return vipOpenAt;
        }
        return generalOpenAt;
    }
}
