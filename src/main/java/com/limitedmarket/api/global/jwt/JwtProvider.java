package com.limitedmarket.api.global.jwt;

import com.limitedmarket.api.domain.member.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    // yml에 저장된 secret 문자열을 JWT 서명에 사용할 수 있는 Key 객체로 변환
    public JwtProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
        this.accessExpiration = jwtProperties.accessTokenExpiration();
        this.refreshExpiration = jwtProperties.refreshTokenExpiration();
    }

    // 토큰 생성 - AccessToken
    public String createAccessToken(Long memberId, Role role) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    // 토큰 생성 - RefreshToken
    public String createRefreshToken(Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    //토큰 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 서명은 맞는데 만료된 토큰
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // 우리 서버가 지원하지 않는 형식의 JWT
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            // 헤더.페이로드.서명 구조가 깨진 토큰 (위변조 의심)
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // 토큰이 null이거나 빈 문자열
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
        }
        return false;
    }

    // 토큰에서 값 추출

    // 토큰에서 memberId 추출
    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 토큰에서 role 추출
    public Role getRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    // 토큰 남은 유효시간 반환 - 로그아웃 시 Redis 블랙리스트에 등록할 TTL
    public long getRemainingExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    // Claims 파싱: Payload -> Java 객체
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                // JWT 파싱 + 서명 검증 + 만료 확인 - 여기서 예외 발생
                .parseSignedClaims(token)
                // getPayload: Claims 객체 반환 (payload에 담긴 데이터)
                .getPayload();
    }
}
