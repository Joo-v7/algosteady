package com.algosteady.judging.domain;

import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

/** 채점 기준의 필수 테스트와 통과 수 */
public record CriterionProgress(Set<String> testIds, int passedTests) {

    /**
     * 상세와 기록 요약에서 같은 필수 테스트를 기준으로 통과 수를 계산한다.
     * @param criterionId 채점 기준 ID
     * @param tests 콘텐츠에 등록된 테스트
     * @param passedTestIds 저장된 통과 테스트 ID
     * @return 필수 테스트와 통과 수
     */
    public static CriterionProgress calculate(String criterionId, Iterable<JsonNode> tests, Set<String> passedTestIds) {
        Set<String> requiredTestIds = new HashSet<>();
        for (JsonNode test : tests) {
            if (criterionId.equals(test.path("criterionId").asString())) {
                requiredTestIds.add(test.path("id").asString());
            }
        }
        int passed = (int) requiredTestIds.stream().filter(passedTestIds::contains).count();
        return new CriterionProgress(Set.copyOf(requiredTestIds), passed);
    }

    /**
     * 기준의 모든 필수 테스트 통과 여부를 반환한다.
     * @return 전체 테스트를 통과했으면 true
     */
    public boolean isPassed() {
        return passedTests == testIds.size();
    }
}
