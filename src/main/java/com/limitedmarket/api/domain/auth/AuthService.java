package com.limitedmarket.api.domain.auth;

import com.limitedmarket.api.domain.auth.dto.LoginRequest;
import com.limitedmarket.api.domain.auth.dto.SignupRequest;
import com.limitedmarket.api.domain.auth.dto.TokenResponse;
import com.limitedmarket.api.domain.member.Member;
import com.limitedmarket.api.domain.member.MemberRepository;
import com.limitedmarket.api.domain.member.MemberStatus;
import com.limitedmarket.api.global.exception.CustomException;
import com.limitedmarket.api.global.exception.ErrorCode;
import com.limitedmarket.api.global.jwt.JwtProperties;
import com.limitedmarket.api.global.jwt.JwtProvider;
import com.limitedmarket.api.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;

    @Transactional
    public void signup(SignupRequest request) {

        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(
                request.email(),
                encodedPassword,
                request.name()
        );

        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.getMemberStatus() == MemberStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        redisService.saveRefreshToken(member.getId(), refreshToken, jwtProperties.refreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)
                || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);

        String storedRefreshToken = redisService.getRefreshToken(memberId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getMemberStatus() == MemberStatus.WITHDRAWN) {
            redisService.deleteRefreshToken(memberId);
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        String newAccessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(member.getId());
        redisService.saveRefreshToken(member.getId(), newRefreshToken, jwtProperties.refreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken, Long memberId) {
        long remainingExpiration = jwtProvider.getRemainingExpiration(accessToken);
        redisService.addToBlacklist(accessToken, remainingExpiration);
        redisService.deleteRefreshToken(memberId);
    }

}
