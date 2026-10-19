package com.algosteady.judging.runner.model;

import com.algosteady.judging.runner.language.LanguageSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 실행 언어·제출 코드·호출 코드·테스트 입력 */
public record ExecutionRequest(
        LanguageSpec language,
        String code,
        String harness,
        List<TestInput> tests
) {
    public ExecutionRequest {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(harness, "harness");
        tests = List.copyOf(tests);
        Set<String> testIds = new HashSet<>();

        for (TestInput test : tests) {
            if (!testIds.add(test.id())) {
                throw new IllegalArgumentException("테스트 ID가 중복됩니다: " + test.id());
            }
        }
    }
}
