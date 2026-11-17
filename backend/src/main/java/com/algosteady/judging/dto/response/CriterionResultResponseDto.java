package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 채점 기준별 진행 및 판정 결과
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CriterionResultResponseDto {

    /** 채점 기준 ID */
    private String criterionId;
    /** 채점 기준 이름 */
    private String title;
    /** 채점 기준 설명 */
    private String description;
    /** 채점 기준의 진행·판정 상태 */
    private String status;
    /** 해당 기준의 전체 테스트 수 */
    private int totalTests;
    /** 결과가 저장된 테스트 수 */
    private int completedTests;
    /** 통과한 테스트 수 */
    private int passedTests;
}
