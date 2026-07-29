package com.limitedmarket.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다"),

    // 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다"),
    MEMBER_WITHDRAWN(HttpStatus.UNAUTHORIZED, "MEMBER_WITHDRAWN", "탈퇴한 회원입니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다"),

    // 상품
    DUPLICATE_PRODUCT_CODE(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT_CODE", "이미 존재하는 상품 코드입니다"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"),
    SALE_NOT_FOUND(HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "판매 정보를 찾을 수 없습니다"),
    SALE_NOT_OPEN(HttpStatus.BAD_REQUEST, "SALE_NOT_OPEN", "아직 판매 시간이 아닙니다"),
    SALE_CLOSED(HttpStatus.BAD_REQUEST, "SALE_CLOSED", "판매가 종료되었습니다"),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "OUT_OF_STOCK", "주문 가능한 재고가 부족합니다"),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다"),
    ORDER_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "ORDER_CANCEL_FAILED", "취소할 수 없는 주문입니다"),
    DUPLICATE_SALE_ITEM(HttpStatus.BAD_REQUEST, "DUPLICATE_SALE_ITEM", "중복된 판매 상품이 포함되어 있습니다"),
    ORDER_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "ORDER_REQUEST_IN_PROGRESS", "동일한 주문 요청을 처리 중입니다"),

    // 결제
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_FAILED", "결제에 실패했습니다"),
    PAYMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "PAYMENT_NOT_ALLOWED", "결제할 수 없는 주문 상태입니다"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", "이미 처리된 결제입니다"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
