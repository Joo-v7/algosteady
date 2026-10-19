package com.algosteady.judging.runner.model;

/** 테스트별 실제 실행 결과 */
public record TestExecutionResult(
        String testId,
        Status status,
        String stdout,
        String stderr,
        Integer exitCode,
        Long executionTimeNanos,
        String message
) {
    public enum Status {
        COMPLETED,
        TIME_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED,
        ENVIRONMENT_ERROR,
        NOT_EXECUTED
    }
}
