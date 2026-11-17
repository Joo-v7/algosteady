package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 서버 학습 날짜의 공통 과제와 현재 회원의 완료 여부
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyLearningResponseDto {

    /** 한국 시간 오전 6시 기준 학습 날짜 */
    private LocalDate learningDate;
    /** 실습 ID */
    private String exerciseId;
    /** 현재 회원의 오늘의 학습 완료 여부 */
    private boolean completed;
    /** 현재 회원의 해당 학습 날짜 내 오늘의 학습 제출 여부 */
    private boolean attempted;
}
