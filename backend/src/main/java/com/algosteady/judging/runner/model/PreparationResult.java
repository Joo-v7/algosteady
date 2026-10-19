package com.algosteady.judging.runner.model;

/** 컴파일·문법 검사 결과 */
public record PreparationResult(
        Status status,
        String stdout,
        String stderr,
        Integer exitCode,
        String message
) {
    public enum Status {
        SUCCEEDED,
        FAILED,
        TIME_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED,
        ENVIRONMENT_ERROR
    }
}
