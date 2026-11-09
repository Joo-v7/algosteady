package com.algosteady.learning.controller;

import com.algosteady.learning.dto.response.DailyLearningResponseDto;
import com.algosteady.learning.dto.response.LearningHistoryResponseDto;
import com.algosteady.learning.service.DailyLearningQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** 오늘의 학습 및 회원별 현황·기록 조회 API */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningQueryController {

    private final DailyLearningQueryService dailyLearningQueryService;

    /**
     * 로그인 회원의 현황과 연도별 완료 기록 조회
     * @param principal 인증된 OAuth 회원
     * @param year 조회 연도, 생략하면 현재 학습 연도
     * @return 본인의 학습 현황과 완료 날짜
     */
    @GetMapping("/history")
    public LearningHistoryResponseDto findHistory(@AuthenticationPrincipal OAuth2User principal, @RequestParam(required = false) Integer year) {
        return dailyLearningQueryService.findHistory(UUID.fromString(principal.getName()), year);
    }

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
