package com.limitedmarket.api.domain.order;

import com.limitedmarket.api.ApiApplication;
import com.limitedmarket.api.domain.member.Member;
import com.limitedmarket.api.domain.member.MemberRepository;
import com.limitedmarket.api.domain.product.Product;
import com.limitedmarket.api.domain.product.ProductRepository;
import com.limitedmarket.api.domain.product.ProductStatus;
import com.limitedmarket.api.domain.product.ProductType;
import com.limitedmarket.api.domain.sale.Sale;
import com.limitedmarket.api.domain.sale.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ApiApplication.class)
@Transactional
class OrderCreateServiceTest {

    @Autowired
    private OrderCreateService orderCreateService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member member;
    private Sale sale;

    @BeforeEach
    void setUp() {
        Product product = Product.create(
                "TEST-" + UUID.randomUUID().toString().substring(0, 8),
                "테스트 상품",
                "설명",
                BigDecimal.valueOf(15000),
                ProductType.SIGNED,
                ProductStatus.ACTIVE
        );
        productRepository.save(product);

        sale = Sale.create(
                product,
                BigDecimal.valueOf(15000),
                100,
                LocalDateTime.now().minusHours(2),  // vipOpenAt
                LocalDateTime.now().minusHours(1),  // generalOpenAt
                LocalDateTime.now().plusDays(1)     // closeAt
        );
        saleRepository.save(sale);

        member = Member.create("test-" + UUID.randomUUID() + "@test.com", "password", "테스터");
        memberRepository.save(member);
    }

    @Test
    @DisplayName("DB 저장 중 예외 발생 시 트랜잭션이 롤백되어 Order가 DB에 남지 않는다")
    void rollback_when_sale_not_found() {
        // given
        long orderCountBefore = orderRepository.count();

        List<Long> invalidSaleIds = List.of(999999L); // 존재하지 않는 saleId
        Map<Long, Integer> quantityMap = Map.of(999999L, 1);

        // when - SALE_NOT_FOUND 예외 발생
        assertThatThrownBy(() ->
                orderCreateService.create(member, invalidSaleIds, quantityMap, UUID.randomUUID().toString())
        ).isInstanceOf(Exception.class);

        // then - Order가 롤백되어 개수 그대로
        assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
    }

    @Test
    @DisplayName("정상 주문 시 Order와 OrderItem이 함께 저장된다")
    void success_order_saves_order_and_items() {
        // given
        List<Long> saleIds = List.of(sale.getId());
        Map<Long, Integer> quantityMap = Map.of(sale.getId(), 1);

        // when
        var response = orderCreateService.create(
                member,
                saleIds,
                quantityMap,
                UUID.randomUUID().toString()
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(orderRepository.count()).isGreaterThan(0);
    }
}
