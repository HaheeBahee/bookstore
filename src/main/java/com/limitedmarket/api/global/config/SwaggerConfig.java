package com.limitedmarket.api.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        // JWT 토큰 받기
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)      // HTTP 방식
                .scheme("bearer")                    // Bearer 토큰 방식
                .bearerFormat("JWT")                 // 토큰 형식은 JWT
                .in(SecurityScheme.In.HEADER)        // Header에 담아서 보냄
                .name("Authorization");              // 헤더 이름은 Authorization

        // Swagger UI
        return new OpenAPI()
                .info(new Info()
                        .title("limited-market API")
                        .description("""
                                한정 상품 선착순 주문 API

                                ## 빠른 체험 순서

                                1. **회원가입·로그인** — 체험에 사용할 회원을 생성하고 로그인합니다.
                                2. **인증** — 로그인 응답의 Access Token을 상단 Authorize에 입력합니다.
                                3. **판매 선택** — 판매 목록에서 주문할 saleId를 확인합니다.
                                4. **주문 생성** — Idempotency-Key에 UUID를 입력해 주문합니다.
                                5. **결제·취소** — 생성된 orderId로 결제(Mock)와 주문 취소를 테스트합니다.

                                > **안내:** 상품·판매 등록은 관리자 전용이며, 주문용 데이터는 미리 등록되어 있습니다.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));
    }
}
