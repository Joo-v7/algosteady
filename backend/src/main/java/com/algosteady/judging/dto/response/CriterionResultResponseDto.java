package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 채점 기준별 진행 및 판정 결과 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CriterionResultResponseDto {

    private String criterionId;

    private String title;

    private String description;

    private String status;

    private int totalTests;

    private int completedTests;

    private int passedTests;
}
