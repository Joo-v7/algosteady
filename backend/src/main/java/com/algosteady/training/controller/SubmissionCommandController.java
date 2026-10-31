package com.algosteady.training.controller;

import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.service.SubmissionCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/** 전체 채점 제출 API */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionCommandController {

    private final SubmissionCommandService submissionCommandService;

    /**
     * 제출을 저장하고 채점 작업 ID 반환
     * @param principal 인증된 OAuth 회원
     * @param request 채점할 코드와 기준 코드 열람 정보
     * @return 등록된 제출 ID와 채점 작업 ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionResponseDto register(@AuthenticationPrincipal OAuth2User principal, @Valid @RequestBody SubmissionRequestDto request) {
        return submissionCommandService.register(UUID.fromString(principal.getName()), request);
    }
}
