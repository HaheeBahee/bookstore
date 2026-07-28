package com.limitedmarket.api.global.jwt;

import com.limitedmarket.api.domain.member.Role;
import com.limitedmarket.api.global.redis.RedisService;
import com.limitedmarket.api.global.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 토큰 꺼내기
        String token = extractToken(request);

        // 2. 토큰 검증 - 서명, 만료시간 등 검증
        if (StringUtils.hasText(token)
                && jwtProvider.validateToken(token)
                && jwtProvider.isAccessToken(token)) {

            // 추가: 블랙리스트 확인 (로그아웃된 토큰이면 인증 정보 저장 안 함)
            if (redisService.isBlacklisted(token)) {
                log.warn("블랙리스트에 등록된 토큰 사용 시도");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 토큰에서 memberId, role 추출
            Long memberId = jwtProvider.getMemberId(token);
            Role role = jwtProvider.getRole(token);

            // 4. CustomUserDetails 생성 (권한 포함)
            CustomUserDetails userDetails = new CustomUserDetails(memberId, role);

            // 5. Authentication 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // 6. SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }


    // Authorization 헤더에서 토큰 추출
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // "Bearer eyJhbG..." 형식인지 확인하고 "eyJhbG..." 부분만 반환
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
