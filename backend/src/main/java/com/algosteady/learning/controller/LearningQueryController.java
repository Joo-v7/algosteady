package com.algosteady.learning.controller;

import com.algosteady.learning.dto.response.DailyLearningResponseDto;
import com.algosteady.learning.service.DailyLearningQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 오늘의 학습 조회 API */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningQueryController {

    private final DailyLearningQueryService dailyLearningQueryService;

    /**
     * 서버 기준 오늘의 학습과 현재 회원의 완료 여부 조회
     * @param principal 인증된 OAuth 회원, 비로그인 조회 허용
     * @return 오늘의 학습 배정과 완료 여부
     */
    @GetMapping("/today")
    public DailyLearningResponseDto findToday(@AuthenticationPrincipal OAuth2User principal) {
        UUID memberId = principal == null ? null : UUID.fromString(principal.getName());
        return dailyLearningQueryService.findToday(memberId);
    }
}
