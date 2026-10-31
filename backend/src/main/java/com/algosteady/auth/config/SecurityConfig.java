package com.algosteady.auth.config;

import com.algosteady.auth.security.SecurityExceptionHandler;
import com.algosteady.auth.service.OAuthLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** OAuth 로그인·세션·API 접근 설정 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuthLoginService oauthLoginService;

    private final SecurityExceptionHandler securityExceptionHandler;

    @Value("${algosteady.frontend-url}")
    private String frontendUrl;

    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;

    /**
     * OAuth2·OIDC 로그인과 세션 기반 API 보안 구성
     * @param http Spring Security HTTP 설정
     * @return 인증·인가 필터 체인
     * @throws Exception 필터 체인 구성 실패
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.requestCache(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        // 학습·예제 실행은 공개, 제출·기록은 로그인 필요
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/me", "/api/submissions/**").authenticated()
                .requestMatchers("/api/exercises/**", "/api/learning/today", "/api/runs", "/api/jobs/**", "/api/auth/providers", "/api/auth/csrf", "/oauth2/**", "/login/**", "/error").permitAll()
                .anyRequest().denyAll());

        http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(oauthLoginService::loadUser)
                        .oidcUserService(oauthLoginService::loadOidcUser))
                .defaultSuccessUrl(frontendUrl + "/exercises", true)
                .failureUrl(frontendUrl + "/login?error=oauth"));

        // POST·CSRF 검증 후 세션과 로그인 쿠키 정리
        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies(sessionCookieName)
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler));

        return http.build();
    }
}
