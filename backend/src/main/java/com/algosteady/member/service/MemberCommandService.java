package com.algosteady.member.service;

import com.algosteady.member.domain.Member;
import com.algosteady.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** OAuth 회원 등록 및 로그인 정보 갱신 */
@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;

    /**
     * 회원과 해당 회원의 제출·채점·학습 기록 삭제
     * @param memberId 인증된 회원 ID
     */
    @Transactional
    public void withdraw(UUID memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));

        // 외래 키의 삭제 연쇄로 본인 기록만 원자적으로 삭제
        memberRepository.delete(member);
        memberRepository.flush();
    }

    /**
     * 제공자 계정으로 회원을 연결하고 최신 프로필 반영
     * @param provider OAuth 등록 ID
     * @param providerSubject 제공자의 고유 사용자 ID
     * @param email 제공자에서 조회한 이메일
     * @param name 제공자에서 조회한 이름
     * @return 로그인한 회원
     */
    @Transactional
    public Member login(String provider, String providerSubject, String email, String name) {
        // 동시 최초 로그인에서도 제공자 계정별 회원 하나만 생성
        memberRepository.saveLogin(UUID.randomUUID(), provider, providerSubject, email, name);

        return memberRepository.findByProviderAndProviderSubject(provider, providerSubject).orElseThrow();
    }
}
