package com.limitedmarket.api.domain.auth;

import com.limitedmarket.api.domain.auth.dto.LoginRequest;
import com.limitedmarket.api.domain.auth.dto.LoginResponse;
import com.limitedmarket.api.domain.auth.dto.SignupRequest;
import com.limitedmarket.api.domain.auth.dto.TokenResponse;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import com.limitedmarket.api.global.jwt.JwtProperties;
import com.limitedmarket.api.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@Tag(name = "1. 회원가입/로그인")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 성공 후 반환된 accessToken을 상단 Authorize 버튼에 입력하세요")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokenResponse.accessToken()));
    }

    @PostMapping("/reissue")
    @Operation(summary = "Access Token 재발급", description = "쿠키의 Refresh Token으로 새 Access Token을 발급합니다")
    public ResponseEntity<LoginResponse> reissue(HttpServletRequest request) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        TokenResponse tokenResponse = authService.reissue(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokenResponse.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(HttpServletRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String accessToken = authHeader.substring(7);

        authService.logout(accessToken, userDetails.getMemberId());

        ResponseCookie deleteCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
