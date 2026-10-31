package com.algosteady.auth.service;

import com.algosteady.auth.dto.response.MemberResponseDto;
import com.algosteady.auth.dto.response.OAuthProviderResponseDto;
import com.algosteady.member.domain.Member;
import com.algosteady.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** OAuth 로그인 수단 및 현재 회원 조회 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {

    private final MemberRepository memberRepository;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final OAuth2ClientProperties oauth2ClientProperties;

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
    public MemberResponseDto findMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));

        return MemberResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .build();
    }
}
