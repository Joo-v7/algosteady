package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 회원의 학습 현황과 선택 연도의 완료 날짜
 *
 * @author Joo
 * @since 2026.11
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LearningHistoryResponseDto {

    /** 서버 기준 현재 학습 날짜 */
    private LocalDate learningDate;
    /** 현재 학습 날짜의 완료 여부 */
    private boolean completedToday;
    /** 현재 연속 완료 일수 */
    private int currentStreak;
    /** 현재 학습 날짜까지의 누적 완료 일수 */
    private int totalCompletedDays;
    /** 조회한 학습 기록 연도 */
    private int year;
    /** 선택 연도의 완료 날짜와 제출 연결 목록 */
    private List<LearningHistoryDayResponseDto> days;
}
