package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * 테스트 입력·기대값·실제 결과·통과 여부
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestResultResponseDto {

    /** 테스트 ID */
    private String testId;
    /** 연결된 채점 기준 ID */
    private String criterionId;
    /** 테스트 설명 */
    private String description;
    /** 테스트 입력 */
    private String input;
    /** 콘텐츠에 정의된 기대 출력과 종료 조건 */
    private JsonNode expected;
    /** 실제 실행 결과 */
    private JsonNode actual;
    /** 테스트 통과 여부 */
    private boolean passed;
}
