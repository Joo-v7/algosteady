package com.algosteady.judging.dto.response;

import lombok.*;
import tools.jackson.databind.JsonNode;

/** 테스트 입력·기대값·실제 결과·통과 여부 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestResultResponseDto {

    private String testId;

    private String criterionId;

    private String description;

    private String input;

    private JsonNode expected;

    private JsonNode actual;

    private boolean passed;
}
