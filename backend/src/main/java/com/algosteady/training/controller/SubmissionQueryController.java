package com.algosteady.training.controller;

import com.algosteady.training.dto.response.SubmissionDetailsResponseDto;
import com.algosteady.training.dto.response.SubmissionPageResponseDto;
import com.algosteady.training.service.SubmissionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 제출 기록 조회 API */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionQueryController {

    private final SubmissionQueryService submissionQueryService;

    /**
     * 과제와 언어별 제출 목록을 페이지로 조회
     * @param principal 인증된 OAuth 회원
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @param page 0부터 시작하는 페이지 번호
     * @param size 한 페이지에 조회할 제출 수
     * @return 제출 목록과 페이지 정보
     */
    @GetMapping
    public SubmissionPageResponseDto findAll(@AuthenticationPrincipal OAuth2User principal, @RequestParam String exerciseId, @RequestParam String language, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return submissionQueryService.findAll(UUID.fromString(principal.getName()), exerciseId, language, page, size);
    }

    /**
     * 제출 원본과 채점 상세 조회
     * @param principal 인증된 OAuth 회원
     * @param id 조회할 제출 ID
     * @return 제출 원본과 연결된 채점 결과
     */
    @GetMapping("/{id}")
    public SubmissionDetailsResponseDto findById(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        return submissionQueryService.findById(UUID.fromString(principal.getName()), id);
    }
}
