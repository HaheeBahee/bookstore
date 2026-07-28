package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.ApiApplication;
import com.limitedmarket.api.domain.member.Member;
import com.limitedmarket.api.domain.member.MemberRepository;
import com.limitedmarket.api.domain.product.Product;
import com.limitedmarket.api.domain.product.ProductRepository;
import com.limitedmarket.api.domain.product.ProductStatus;
import com.limitedmarket.api.domain.product.ProductType;
import com.limitedmarket.api.domain.sale.RedisStockService;
import com.limitedmarket.api.domain.sale.Sale;
import com.limitedmarket.api.domain.sale.SaleRepository;
import com.limitedmarket.api.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ApiApplication.class)
class OrderCancelRedisTest {

    @Autowired
    private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RedisStockService redisStockService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;

    private Member member;
    private Sale sale;

    @BeforeEach
    void setUp() {
        Product product = Product.create(
                "TEST-" + UUID.randomUUID().toString().substring(0, 8),
                "테스트 상품", "설명", BigDecimal.valueOf(15000),
                ProductType.SIGNED, ProductStatus.ACTIVE);
        productRepository.save(product);

        sale = Sale.create(product, BigDecimal.valueOf(15000), 100,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));
        saleRepository.save(sale);

        member = Member.create(
                "test-" + UUID.randomUUID() + "@test.com",
                "password", "테스터");
        memberRepository.save(member);

        redisStockService.initStockIfAbsent(sale.getId(), 100);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("sale:stock:" + sale.getId());
        orderItemRepository.deleteAll();
        orderStatusHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.delete(member);
    }

    @Test
    @DisplayName("주문 취소 성공 시 Redis 재고가 복구된다")
    void cancel_success_redis_restored() {
        // given
        Order order = Order.create(
                member,
                BigDecimal.valueOf(30000),
                UUID.randomUUID().toString()
        );
        orderRepository.save(order);
        OrderItem orderItem = OrderItem.create(order, sale, 2);
        orderItemRepository.save(orderItem);
        sale.decreaseStock(2);
        saleRepository.save(sale);
        redisTemplate.opsForValue().decrement("sale:stock:" + sale.getId(), 2);

        // when
        orderService.cancel(order.getId(), member.getId());

        // then
        String afterCancel = redisTemplate.opsForValue().get("sale:stock:" + sale.getId());
        assertThat(afterCancel).isEqualTo("100");
    }

    @Test
    @DisplayName("취소 실패(없는 주문) 시 Redis 재고는 변하지 않는다")
    void cancel_fail_redis_unchanged() {
        // given
        String before = redisTemplate.opsForValue().get("sale:stock:" + sale.getId());

        // when & then
        assertThatThrownBy(() -> orderService.cancel(999999L, member.getId()))
                .isInstanceOf(CustomException.class);

        String after = redisTemplate.opsForValue().get("sale:stock:" + sale.getId());
        assertThat(after).isEqualTo(before);
    }
}
