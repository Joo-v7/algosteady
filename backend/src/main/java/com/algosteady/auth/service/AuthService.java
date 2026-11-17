package com.algosteady.auth.service;

import com.algosteady.auth.dto.response.MemberResponseDto;
import com.algosteady.auth.dto.response.OAuthProviderResponseDto;
import com.algosteady.member.domain.Member;
import com.algosteady.member.repository.MemberRepository;
import com.algosteady.member.service.MemberService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * OAuth 로그인 및 현재 회원 조회
 *
 * @author Joo
 * @since 2026.11
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2ClientProperties oauth2ClientProperties;
    private final MemberService memberService;

    /**
     * 서버에 등록된 OAuth 제공자와 로그인 시작 경로 조회
     * @return 로그인 화면에 표시할 제공자 목록
     */
    public List<OAuthProviderResponseDto> findProviders() {
        List<OAuthProviderResponseDto> response = new ArrayList<>();

        for (String id : oauth2ClientProperties.getRegistration().keySet().stream().sorted().toList()) {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(id);
            response.add(OAuthProviderResponseDto.builder()
                    .id(id)
                    .name(registration.getClientName())
                    .authorizationUrl("/oauth2/authorization/" + id)
                    .build());
        }

        return response;
    }

    /**
     * 인증된 세션의 회원 ID로 기본 정보 조회
     * @param memberId 로그인한 회원 ID
     * @return 현재 회원의 이름과 이메일
     */
    @Transactional(readOnly = true)
    public MemberResponseDto findMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));

        return MemberResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .build();
    }

    /**
     * OAuth2 사용자 조회 후 회원 ID를 세션 인증 이름으로 설정
     * @param request 제공자 등록 정보와 액세스 토큰
     * @return 회원과 연결된 OAuth2 사용자
     */
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User user = new DefaultOAuth2UserService().loadUser(request);
        Member member = memberService.login(request.getClientRegistration().getRegistrationId(), user.getName(), user.getAttribute("email"), user.getAttribute("name"));
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
        Member member = memberService.login(request.getClientRegistration().getRegistrationId(), user.getSubject(), user.getEmail(), user.getFullName());
        Map<String, Object> claims = new HashMap<>(user.getClaims());
        claims.put("member_id", member.getId().toString());

        return new DefaultOidcUser(user.getAuthorities(), user.getIdToken(), new OidcUserInfo(claims), "member_id");
    }
}
