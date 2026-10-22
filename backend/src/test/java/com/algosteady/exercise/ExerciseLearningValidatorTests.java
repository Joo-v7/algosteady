package com.algosteady.exercise;

import com.algosteady.exercise.service.ExerciseLearningValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

/** 학습 데이터 형식과 노드 연결 검증 */
class ExerciseLearningValidatorTests {

    private final ExerciseLearningValidator exerciseLearningValidator = new ExerciseLearningValidator();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @ParameterizedTest
    @MethodSource("com.algosteady.support.ExerciseContentSource#contents")
    @DisplayName("등록된 모든 콘텐츠의 학습 단계를 허용합니다.")
    void validate_success_withExistingContent(Resource resource) throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(resource.getContentAsString(StandardCharsets.UTF_8));

        // when / then
        Assertions.assertDoesNotThrow(() -> exerciseLearningValidator.validate(exercise));
    }

    @ParameterizedTest
    @MethodSource("invalidSteps")
    @DisplayName("필수 필드 오류와 잘못된 노드 연결을 단계 위치와 함께 거부합니다.")
    void validate_fail_withInvalidStep(String exerciseId, String field, String value) throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/" + exerciseId + "/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(0);
        step.set(field, jsonMapper.readTree(value));

        // when / then
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
            Assertions.assertTrue(failure.getMessage().contains(exerciseId + " 1단계"));
        });
    }

    @Test
    @DisplayName("단계가 없는 학습 콘텐츠를 거부합니다.")
    void validate_fail_withEmptySteps() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/stack-array/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ((ObjectNode) exercise.path("learning")).putArray("steps");

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
    }

    static Stream<Arguments> invalidSteps() {
        return Stream.of(
                Arguments.of("set-union-find", "parents", "[1,0]"),
                Arguments.of("set-union-find", "parents", "[0]"),
                Arguments.of("set-union-find", "parents", "[-2]"),
                Arguments.of("set-union-find", "parents", "[8]"),
                Arguments.of("set-union-find", "activeNode", "99"),
                Arguments.of("set-union-find", "visited", "[99]"),
                Arguments.of("tree-binary", "tree", "[null,10]"),
                Arguments.of("tree-binary", "activeNode", "0"),
                Arguments.of("tree-binary", "tree", "[\"bad\"]"),
                Arguments.of("heap-max", "values", "[5]"),
                Arguments.of("hash-chaining", "buckets", "[]"),
                Arguments.of("hash-chaining", "activeBucket", "7"),
                Arguments.of("search-binary-iterative", "low", "-1"),
                Arguments.of("search-binary-recursive", "high", "99"),
                Arguments.of("stack-array", "values", "null"),
                Arguments.of("stack-array", "top", "0"),
                Arguments.of("queue-circular", "front", "5"),
                Arguments.of("recursion-factorial", "trace", "[1]"));
    }
}
