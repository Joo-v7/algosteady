package com.algosteady.judging.controller;

import com.algosteady.judging.dto.request.RunRequestDto;
import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.dto.response.RunResponseDto;
import com.algosteady.judging.service.JudgingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실행 요청 및 작업 결과 조회 API
 *
 * @author Joo
 * @since 2026.11
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JudgingController {

    private final JudgingService judgingService;

    /**
     * 요청을 저장하고 비동기 작업 ID 반환
     * @param request 실행할 코드와 공개 예시 정보
     * @return 등록된 실행 작업 ID
     */
    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResponseDto register(@Valid @RequestBody RunRequestDto request) {
        return judgingService.register(request);
    }

    /**
     * 실행 원본과 컴파일 및 테스트 결과 조회
     * @param principal 인증된 OAuth 회원, 공개 실행 조회 시 비로그인 허용
     * @param id 조회할 실행 작업 ID
     * @return 작업 원본과 컴파일·테스트 결과
     */
    @GetMapping("/jobs/{id}")
    public JobResponseDto findById(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        UUID memberId = principal == null ? null : UUID.fromString(principal.getName());
        return judgingService.findById(id, memberId);
    }
}
