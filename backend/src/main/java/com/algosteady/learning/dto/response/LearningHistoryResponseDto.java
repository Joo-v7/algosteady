package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 회원의 학습 현황과 선택 연도의 완료 날짜 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LearningHistoryResponseDto {

    private LocalDate learningDate;

    private boolean completedToday;

    private int currentStreak;

    private int totalCompletedDays;

    private int year;

    private List<LearningHistoryDayResponseDto> days;
}
