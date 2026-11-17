package com.algosteady.auth.controller;

import com.algosteady.auth.dto.response.CsrfTokenResponseDto;
import com.algosteady.auth.dto.response.MemberResponseDto;
import com.algosteady.auth.dto.response.OAuthProviderResponseDto;
import com.algosteady.auth.service.AuthService;
import com.algosteady.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 정보 조회 및 회원탈퇴 API
 *
 * @author Joo
 * @since 2026.11
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;

    private final AuthService authService;
    private final MemberService memberService;
    private final SessionRegistry sessionRegistry;

    /**
     * 로그인 화면의 OAuth 제공자 조회
     * @return 등록된 OAuth 제공자 목록
     */
    @GetMapping("/providers")
    public List<OAuthProviderResponseDto> findProviders() {
        return authService.findProviders();
    }

    /**
     * 현재 인증 세션의 회원 조회
     * @param principal OAuth 로그인으로 생성한 인증 사용자
     * @return 로그인한 회원의 기본 정보
     */
    @GetMapping("/me")
    public MemberResponseDto findMember(@AuthenticationPrincipal OAuth2User principal) {
        return authService.findMember(UUID.fromString(principal.getName()));
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

    /**
     * 본인 계정·기록 삭제 후 모든 로그인 세션 만료
     * @param principal 인증된 OAuth 회원
     * @param authentication 현재 로그인 인증
     * @param request 현재 세션 요청
     * @param response 로그인 쿠키를 정리할 응답
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal OAuth2User principal, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        memberService.withdraw(UUID.fromString(principal.getName()));

        // 프로필 갱신 전후의 인증 객체를 포함한 본인 세션 전체 만료
        for (Object registered : sessionRegistry.getAllPrincipals()) {
            if (registered instanceof OAuth2User user && user.getName().equals(principal.getName())) {
                for (SessionInformation session : sessionRegistry.getAllSessions(registered, false)) {
                    session.expireNow();
                }
            }
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler(sessionCookieName).logout(request, response, authentication);
    }
}
