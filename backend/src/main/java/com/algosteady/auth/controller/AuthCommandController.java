package com.algosteady.auth.controller;

import com.algosteady.member.service.MemberCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 회원탈퇴 및 로그인 세션 정리 API */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthCommandController {

    private final MemberCommandService memberCommandService;

    private final SessionRegistry sessionRegistry;

    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;

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
        memberCommandService.withdraw(UUID.fromString(principal.getName()));

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
