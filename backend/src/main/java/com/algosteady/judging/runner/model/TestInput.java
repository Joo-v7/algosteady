package com.algosteady.judging.runner.model;

import java.util.Objects;

/** 테스트 ID 및 실행 입력 */
public record TestInput(
        String id,
        String input
) {
    public TestInput {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("테스트 ID가 필요합니다.");
        }
        Objects.requireNonNull(input, "input");
    }
}
