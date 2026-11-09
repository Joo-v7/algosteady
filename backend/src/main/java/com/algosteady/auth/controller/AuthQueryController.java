package com.algosteady.auth.controller;

import com.algosteady.auth.dto.response.CsrfTokenResponseDto;
import com.algosteady.auth.dto.response.MemberResponseDto;
import com.algosteady.auth.dto.response.OAuthProviderResponseDto;
import com.algosteady.auth.service.AuthQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** OAuth 로그인 수단·현재 회원·CSRF 토큰 조회 API */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthQueryController {

    private final AuthQueryService authQueryService;

    /**
     * 로그인 화면의 OAuth 제공자 조회
     * @return 등록된 OAuth 제공자 목록
     */
    @GetMapping("/providers")
    public List<OAuthProviderResponseDto> findProviders() {
        return authQueryService.findProviders();
    }

    /**
     * 현재 인증 세션의 회원 조회
     * @param principal OAuth 로그인으로 생성한 인증 사용자
     * @return 로그인한 회원의 기본 정보
     */
    @GetMapping("/me")
    public MemberResponseDto findMember(@AuthenticationPrincipal OAuth2User principal) {
        return authQueryService.findMember(UUID.fromString(principal.getName()));
    }

    /**
     * 변경 요청에 사용할 세션 CSRF 토큰 조회
     * @param csrfToken Spring Security가 제공한 요청 토큰
     * @return CSRF 토큰과 요청 헤더 이름
     */
    @GetMapping("/csrf")
    public CsrfTokenResponseDto findCsrfToken(CsrfToken csrfToken) {
        return CsrfTokenResponseDto.builder()
                .headerName(csrfToken.getHeaderName())
                .token(csrfToken.getToken())
                .build();
    }
}
