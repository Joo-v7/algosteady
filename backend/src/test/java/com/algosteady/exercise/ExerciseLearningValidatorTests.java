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

    @Test
    @DisplayName("이미 배치한 퀸들의 대각선 충돌을 거부합니다.")
    void validate_fail_withAttackingQueenPrefix() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/backtracking-nqueen/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(0);
        step.put("row", 2);
        step.set("queens", jsonMapper.readTree("[0,1,-1,-1]"));

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
    }

    @Test
    @DisplayName("검사 후보의 실제 충돌과 다른 안전 표시를 거부합니다.")
    void validate_fail_withIncorrectQueenSafety() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/backtracking-nqueen/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(0);
        step.put("row", 1);
        step.set("queens", jsonMapper.readTree("[0,-1,-1,-1]"));
        step.put("candidateColumn", 1);
        step.put("safe", true);

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
    }

    @Test
    @DisplayName("선택한 활동들이 서로 겹치는 학습 단계를 거부합니다.")
    void validate_fail_withOverlappingActivities() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/greedy-activity-selection/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(0);
        step.put("processed", 6);
        step.set("selected", jsonMapper.readTree("[2,1]"));
        step.put("lastEnd", 6);

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("선택 활동 겹침"));
    }

    @Test
    @DisplayName("타뷸레이션 단계에 재귀 호출 스택이 있는 데이터를 거부합니다.")
    void validate_fail_withTabulationCallStack() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/dp-fibonacci/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").valueStream()
                .filter(item -> item.path("method").asString().equals("tabulation"))
                .findFirst()
                .orElseThrow();
        step.set("pending", jsonMapper.readTree("[0]"));

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("타뷸레이션 호출 스택"));
    }

    @Test
    @DisplayName("현재 행을 참조해 물건을 중복 사용한 학습 표를 거부합니다.")
    void validate_fail_withRepeatedKnapsackItem() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/dp-knapsack/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(3);
        step.set("table", jsonMapper.readTree("[[0,0,0,0,0],[0,15,30,45,60],[null,null,null,null,null],[null,null,null,null,null]]"));

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("배낭 최적 가치"));
    }

    @Test
    @DisplayName("이전 행과 다른 포함 가치 설명을 거부합니다.")
    void validate_fail_withIncorrectKnapsackComparison() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/dp-knapsack/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(6);
        ((ObjectNode) step.path("comparison")).put("included", 20);

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("배낭 포함 가치"));
    }

    @ParameterizedTest
    @MethodSource("invalidLcsTables")
    @DisplayName("잘못된 LCS 기저값·대각선 갱신·불일치 초기화·계산 순서를 거부합니다.")
    void validate_fail_withInvalidLcsTable(int stepIndex, String table) throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/dp-lcs/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(stepIndex);
        step.set("table", jsonMapper.readTree(table));

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
    }

    static Stream<Arguments> invalidLcsTables() {
        return Stream.of(
                Arguments.of(1, "[[1,0,0,0],[0,null,null,null],[0,null,null,null],[0,null,null,null]]"),
                Arguments.of(2, "[[0,0,0,0],[0,2,null,null],[0,null,null,null],[0,null,null,null]]"),
                Arguments.of(3, "[[0,0,0,0],[0,1,0,null],[0,null,null,null],[0,null,null,null]]"),
                Arguments.of(2, "[[null,0,0,0],[0,1,null,null],[0,null,null,null],[0,null,null,null]]"));
    }

    @Test
    @DisplayName("실제로 정답 쌍이 남아 있는 투 포인터 실패 단계를 거부합니다.")
    void validate_fail_withIncorrectPairAbsence() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/two-pointer-pair/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(9);
        step.put("target", 15);

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("투 포인터 탐색 실패 판정"));
    }

    @ParameterizedTest
    @MethodSource("invalidWindowUpdates")
    @DisplayName("슬라이딩 윈도의 이전 합·최대 합·완료 반환값 오류를 거부합니다.")
    void validate_fail_withInvalidWindowUpdate(int stepIndex, String field, String value) throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/sliding-window-max-sum/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(stepIndex);
        step.set(field, jsonMapper.readTree(value));

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
    }

    static Stream<Arguments> invalidWindowUpdates() {
        return Stream.of(
                Arguments.of(3, "previousSum", "0"),
                Arguments.of(4, "best", "4"),
                Arguments.of(5, "best", "3"),
                Arguments.of(6, "result", "3"),
                Arguments.of(3, "start", "0"));
    }

    @Test
    @DisplayName("음수 구간의 최대 합을 0으로 초기화한 학습 단계를 거부합니다.")
    void validate_fail_withZeroBestForNegativeWindow() throws Exception {
        // given
        ObjectNode exercise = (ObjectNode) jsonMapper.readTree(new ClassPathResource("content/sliding-window-max-sum/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        ObjectNode step = (ObjectNode) exercise.path("learning").path("steps").get(0);
        step.set("values", jsonMapper.readTree("[-5,-2]"));
        step.put("k", 2);
        step.put("end", 1);
        step.put("sum", -7);
        step.put("best", 0);

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseLearningValidator.validate(exercise));
        Assertions.assertTrue(failure.getMessage().contains("슬라이딩 윈도 최대 합"));
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
                Arguments.of("recursion-factorial", "trace", "[1]"),
                Arguments.of("sort-selection", "values", "null"),
                Arguments.of("sort-selection", "values", "[\"bad\"]"),
                Arguments.of("sort-selection", "active", "[-1]"),
                Arguments.of("sort-selection", "sorted", "[100]"),
                Arguments.of("sort-merge", "trace", "[1]"),
                Arguments.of("graph-matrix", "adjacency", "null"),
                Arguments.of("graph-matrix", "adjacency", "[[1],[]]"),
                Arguments.of("graph-list", "adjacency", "[[0]]"),
                Arguments.of("graph-list", "adjacency", "[[1,1],[0]]"),
                Arguments.of("graph-list", "adjacency", "[[2],[0]]"),
                Arguments.of("graph-dfs", "visited", "[-1]"),
                Arguments.of("graph-dfs", "activeNode", "99"),
                Arguments.of("graph-bfs", "frontier", "[99]"),
                Arguments.of("graph-bfs", "frontierLabel", "null"),
                Arguments.of("graph-kruskal", "weights", "[]"),
                Arguments.of("graph-kruskal", "weights", "[[0,1],[1,0]]"),
                Arguments.of("graph-kruskal", "weights", "[[0,1,3,null],[2,0,2,5],[3,2,0,4],[null,5,4,0]]"),
                Arguments.of("graph-prim", "weights", "[[0,null,3,null],[null,0,2,5],[3,2,0,4],[null,5,4,0]]"),
                Arguments.of("graph-prim", "selectedEdges", "[[0,3]]"),
                Arguments.of("graph-prim", "selectedEdges", "[[0,1],[1,0]]"),
                Arguments.of("graph-kruskal", "selectedEdges", "[[0,99]]"),
                Arguments.of("graph-kruskal", "activeEdge", "[0]"),
                Arguments.of("graph-kruskal", "activeEdge", "[0,3]"),
                Arguments.of("graph-prim", "totalWeight", "1"),
                Arguments.of("graph-dijkstra", "distances", "[0]"),
                Arguments.of("graph-dijkstra", "distances", "[0,\"INF\",null,null,null]"),
                Arguments.of("graph-dijkstra", "source", "5"),
                Arguments.of("graph-dijkstra", "activeEdge", "[1,0]"),
                Arguments.of("graph-bellman-ford", "weights", "[]"),
                Arguments.of("graph-bellman-ford", "distances", "null"),
                Arguments.of("graph-bellman-ford", "source", "-1"),
                Arguments.of("graph-bellman-ford", "activeEdge", "[3,2]"),
                Arguments.of("graph-floyd-warshall", "distanceMatrix", "null"),
                Arguments.of("graph-floyd-warshall", "distanceMatrix", "[[0]]"),
                Arguments.of("graph-floyd-warshall", "distanceMatrix", "[[0,1,2,3],[null],[null,null,0,1],[null,null,null,0]]"),
                Arguments.of("graph-floyd-warshall", "distanceMatrix", "[[0,1,2,\"INF\"],[null,0,2,3],[null,null,0,1],[null,null,null,0]]"),
                Arguments.of("graph-floyd-warshall", "intermediate", "-1"),
                Arguments.of("graph-floyd-warshall", "intermediate", "4"),
                Arguments.of("graph-topological", "inDegrees", "null"),
                Arguments.of("graph-topological", "inDegrees", "[0]"),
                Arguments.of("graph-topological", "inDegrees", "[0,0,1,1,2,1]"),
                Arguments.of("graph-topological", "inDegrees", "[0,0,-1,1,2,1]"),
                Arguments.of("graph-topological", "frontier", "[0,0]"),
                Arguments.of("graph-topological", "frontier", "[2]"),
                Arguments.of("graph-topological", "visited", "[0,0]"),
                Arguments.of("graph-topological", "visited", "[0]"),
                Arguments.of("string-brute-force", "text", "null"),
                Arguments.of("string-brute-force", "offset", "-1"),
                Arguments.of("string-brute-force", "offset", "8"),
                Arguments.of("string-brute-force", "patternIndex", "6"),
                Arguments.of("string-transform", "tokens", "[1]"),
                Arguments.of("string-transform", "output", "1"),
                Arguments.of("trie-basic", "nodes", "[]"),
                Arguments.of("trie-basic", "nodes", "[{\"prefix\":\"\",\"terminal\":false},{\"prefix\":\"ab\",\"terminal\":true}]"),
                Arguments.of("trie-basic", "nodes", "[{\"prefix\":\"\",\"terminal\":false},{\"prefix\":\"\",\"terminal\":true}]"),
                Arguments.of("trie-basic", "nodes", "[{\"prefix\":\"\",\"terminal\":1}]"),
                Arguments.of("trie-basic", "word", "\"A\""),
                Arguments.of("trie-basic", "activePrefix", "\"missing\""),
                Arguments.of("exhaustive-permutations", "values", "[1,1]"),
                Arguments.of("exhaustive-permutations", "selected", "[4]"),
                Arguments.of("exhaustive-permutations", "selected", "[1,1]"),
                Arguments.of("exhaustive-combinations", "depth", "-1"),
                Arguments.of("exhaustive-combinations", "depth", "5"),
                Arguments.of("exhaustive-subsets", "activeValue", "0"),
                Arguments.of("exhaustive-subsets", "trace", "[1]"),
                Arguments.of("backtracking-nqueen", "size", "0"),
                Arguments.of("backtracking-nqueen", "size", "11"),
                Arguments.of("backtracking-nqueen", "queens", "[]"),
                Arguments.of("backtracking-nqueen", "queens", "[0,-1,-1,-1]"),
                Arguments.of("backtracking-nqueen", "row", "5"),
                Arguments.of("backtracking-nqueen", "candidateColumn", "4"),
                Arguments.of("backtracking-nqueen", "solutions", "-1"),
                Arguments.of("greedy-activity-selection", "activities", "null"),
                Arguments.of("greedy-activity-selection", "activities", "[{\"id\":0,\"start\":2,\"end\":2}]"),
                Arguments.of("greedy-activity-selection", "activities", "[{\"id\":0,\"start\":0,\"end\":1},{\"id\":0,\"start\":1,\"end\":2}]"),
                Arguments.of("greedy-activity-selection", "selected", "[99]"),
                Arguments.of("greedy-activity-selection", "processed", "7"),
                Arguments.of("greedy-activity-selection", "lastEnd", "0"),
                Arguments.of("sliding-window-max-sum", "values", "[]"),
                Arguments.of("sliding-window-max-sum", "k", "0"),
                Arguments.of("sliding-window-max-sum", "k", "7"),
                Arguments.of("sliding-window-max-sum", "start", "-1"),
                Arguments.of("sliding-window-max-sum", "end", "6"),
                Arguments.of("sliding-window-max-sum", "sum", "0"),
                Arguments.of("sliding-window-max-sum", "best", "0"),
                Arguments.of("sliding-window-max-sum", "phase", "\"unknown\""),
                Arguments.of("sliding-window-max-sum", "previousSum", "0"),
                Arguments.of("two-pointer-pair", "values", "[2,1,4,7,11,15]"),
                Arguments.of("two-pointer-pair", "values", "null"),
                Arguments.of("two-pointer-pair", "left", "-1"),
                Arguments.of("two-pointer-pair", "right", "6"),
                Arguments.of("two-pointer-pair", "sum", "0"),
                Arguments.of("two-pointer-pair", "decision", "\"move-left\""),
                Arguments.of("two-pointer-pair", "result", "1"),
                Arguments.of("two-pointer-pair", "target", "null"),
                Arguments.of("dp-lcs", "problem", "\"knapsack\""),
                Arguments.of("dp-lcs", "a", "null"),
                Arguments.of("dp-lcs", "a", "\"abc\""),
                Arguments.of("dp-lcs", "b", "\"가\""),
                Arguments.of("dp-lcs", "table", "[]"),
                Arguments.of("dp-lcs", "table", "[[0],[0],[0],[0]]"),
                Arguments.of("dp-lcs", "activeCell", "[4,0]"),
                Arguments.of("dp-lcs", "activeCell", "[0,4]"),
                Arguments.of("dp-lcs", "activeCell", "[-1,0]"),
                Arguments.of("dp-knapsack", "problem", "\"fibonacci\""),
                Arguments.of("dp-knapsack", "capacity", "-1"),
                Arguments.of("dp-knapsack", "capacity", "101"),
                Arguments.of("dp-knapsack", "items", "null"),
                Arguments.of("dp-knapsack", "items", "[{\"weight\":0,\"value\":1}]"),
                Arguments.of("dp-knapsack", "items", "[{\"weight\":1,\"value\":1001}]"),
                Arguments.of("dp-knapsack", "table", "[]"),
                Arguments.of("dp-knapsack", "table", "[[0],[0],[0],[0]]"),
                Arguments.of("dp-knapsack", "activeCell", "[4,0]"),
                Arguments.of("dp-knapsack", "activeCell", "[0,5]"),
                Arguments.of("dp-knapsack", "comparison", "{}"),
                Arguments.of("dp-fibonacci", "problem", "\"knapsack\""),
                Arguments.of("dp-fibonacci", "method", "\"unknown\""),
                Arguments.of("dp-fibonacci", "target", "-1"),
                Arguments.of("dp-fibonacci", "target", "47"),
                Arguments.of("dp-fibonacci", "values", "[0]"),
                Arguments.of("dp-fibonacci", "values", "[0,1,2,null,null,null]"),
                Arguments.of("dp-fibonacci", "activeIndex", "6"),
                Arguments.of("dp-fibonacci", "pending", "[6]"),
                Arguments.of("dp-fibonacci", "pending", "[5,5]"),
                Arguments.of("dp-fibonacci", "reused", "true"));
    }
}
