package com.algosteady.judging.runner.model;

import java.util.List;
import java.util.Objects;

/** 컴파일 및 테스트별 실행 결과 */
public record ExecutionResult(
        PreparationResult preparation,
        List<TestExecutionResult> tests
) {
    public ExecutionResult {
        Objects.requireNonNull(preparation, "preparation");
        tests = List.copyOf(tests);
    }
}
