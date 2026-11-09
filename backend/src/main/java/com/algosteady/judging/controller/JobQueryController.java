package com.algosteady.judging.controller;

import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.service.JudgingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 실행 작업 조회 API */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobQueryController {

    private final JudgingQueryService judgingQueryService;

    /**
     * 실행 원본과 컴파일 및 테스트 결과 조회
     * @param principal 인증된 OAuth 회원, 공개 실행 조회 시 비로그인 허용
     * @param id 조회할 실행 작업 ID
     * @return 작업 원본과 컴파일·테스트 결과
     */
    @GetMapping("/{id}")
    public JobResponseDto findById(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        UUID memberId = principal == null ? null : UUID.fromString(principal.getName());
        return judgingQueryService.findById(id, memberId);
    }
}
