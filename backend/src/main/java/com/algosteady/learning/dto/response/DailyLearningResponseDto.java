package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 서버 학습 날짜의 공통 과제와 현재 회원의 완료 여부 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyLearningResponseDto {

    private LocalDate learningDate;

    private String exerciseId;

    private boolean completed;
}
