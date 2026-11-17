package com.algosteady.judging.runner.model;

/**
 * 컴파일·문법 검사 결과
 *
 * @author Joo
 * @since 2026.10
 */
public record PreparationResult(
        Status status,
        String stdout,
        String stderr,
        Integer exitCode,
        String message
) {
    /**
     * 컴파일·실행 준비 상태
     */
    public enum Status {
        SUCCEEDED,
        FAILED,
        TIME_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED,
        ENVIRONMENT_ERROR
    }
}
