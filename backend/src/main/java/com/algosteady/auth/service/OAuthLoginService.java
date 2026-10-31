package com.algosteady.auth.service;

import com.algosteady.member.domain.Member;
import com.algosteady.member.service.MemberCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** OAuth2·OIDC 사용자 정보와 내부 회원 연결 */
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final MemberCommandService memberCommandService;

    /**
     * OAuth2 사용자 조회 후 회원 ID를 세션 인증 이름으로 설정
     * @param request 제공자 등록 정보와 액세스 토큰
     * @return 회원과 연결된 OAuth2 사용자
     */
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User user = new DefaultOAuth2UserService().loadUser(request);
        Member member = memberCommandService.login(request.getClientRegistration().getRegistrationId(), user.getName(), user.getAttribute("email"), user.getAttribute("name"));
        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        attributes.put("member_id", member.getId().toString());

        return new DefaultOAuth2User(user.getAuthorities(), attributes, "member_id");
    }

    /**
     * OIDC 사용자 조회 후 검증된 제공자 사용자 ID와 회원 연결
     * @param request 검증된 ID 토큰과 제공자 등록 정보
     * @return 회원과 연결된 OIDC 사용자
     */
    public OidcUser loadOidcUser(OidcUserRequest request) {
        OidcUser user = new OidcUserService().loadUser(request);
        Member member = memberCommandService.login(request.getClientRegistration().getRegistrationId(), user.getSubject(), user.getEmail(), user.getFullName());
        Map<String, Object> claims = new HashMap<>(user.getClaims());
        claims.put("member_id", member.getId().toString());

        return new DefaultOidcUser(user.getAuthorities(), user.getIdToken(), new OidcUserInfo(claims), "member_id");
    }
}
