package com.algosteady.judging.runner.service;

import com.algosteady.judging.runner.language.LanguageSpec;
import com.algosteady.judging.runner.model.ExecutionRequest;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.runner.model.TestInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 콘텐츠 기준 코드와 대표 오답의 실제 실행 검증 */
@EnabledIfSystemProperty(named = "algosteady.docker.tests", matches = "true")
class ExerciseContentDockerTests {

    private final DockerExecutionFixture fixture = new DockerExecutionFixture();

    @ParameterizedTest
    @MethodSource("com.algosteady.support.ExerciseContentSource#contents")
    @DisplayName("과제 기준 코드의 전체 테스트를 실제 실행으로 검증합니다.")
    void execute_success_withContentReference(Resource contentResource) throws Exception {
        // given
        JsonNode metadata = JsonMapper.builder().build().readTree(contentResource.getContentAsString(StandardCharsets.UTF_8));
        JsonNode language = metadata.path("languages").path("c");
        JsonNode tests = JsonMapper.builder().build().readTree(contentResource.createRelative(language.path("tests").asString()).getContentAsString(StandardCharsets.UTF_8));
        List<TestInput> inputs = new ArrayList<>();
        tests.forEach(test -> inputs.add(new TestInput(test.path("id").asString(), test.path("input").asString())));
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C,
                contentResource.createRelative(language.path("reference").asString()).getContentAsString(StandardCharsets.UTF_8),
                contentResource.createRelative(language.path("harness").asString()).getContentAsString(StandardCharsets.UTF_8), inputs);

        List<TestExecutionResult> delivered = new ArrayList<>();

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, delivered::add);

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        Assertions.assertEquals(tests.size(), result.tests().size());
        Assertions.assertEquals(result.tests(), delivered);
        for (int index = 0; index < tests.size(); index++) {
            JsonNode expected = tests.get(index).path("expected");
            TestExecutionResult actual = result.tests().get(index);
            Assertions.assertEquals(inputs.get(index).id(), actual.testId());
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status(), actual.toString());
            Assertions.assertEquals(expected.path("stdout").asString(), actual.stdout(), actual.testId());
            Assertions.assertEquals(expected.path("exitCode").asInt(), actual.exitCode(), actual.testId());
            Assertions.assertEquals(expected.path("stderrRequired").asBoolean(), !actual.stderr().isBlank(), actual.testId());
            Assertions.assertNotNull(actual.executionTimeNanos());
            Assertions.assertTrue(actual.executionTimeNanos() >= 0);
        }
    }

    @Test
    @DisplayName("용량 확장 누락·값 손실·조회 시 삭제 오답을 구분합니다.")
    void execute_success_detectsDynamicStackMistakes() throws Exception {
        // given
        Path dynamicContent = Path.of("../content/stack-dynamic/c");
        String reference = Files.readString(dynamicContent.resolve("reference.c"));
        String harness = Files.readString(dynamicContent.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(dynamicContent.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "growth-boundaries", reference.replace("if (is_full(s)) {", "if (is_full(s)) { return;"),
                "growth-preserves-values", reference.replace("s->data = data;", "s->data = data; s->data[0] = -999;"),
                "peek-one", reference.replace("return s->data[s->top];", "return s->data[(s->top)--];"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            Assertions.assertNotEquals(test.path("expected").path("stdout").asString(), result.tests().getFirst().stdout(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("선형 큐의 FIFO·앞 공간 재사용·오류 종료 오답을 구분합니다.")
    void execute_success_detectsLinearQueueMistakes() throws Exception {
        // given
        Path queueContent = Path.of("../content/queue-linear/c");
        String reference = Files.readString(queueContent.resolve("reference.c"));
        String harness = Files.readString(queueContent.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(queueContent.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "dequeue-fifo", reference.replace("return q->data[++(q->front)];", "q->front++; return q->data[q->rear];"),
                "linear-full-after-delete", reference.replace("if (is_full(q)) {", "if (is_full(q) && q->front == -1) {").replace("q->data[++(q->rear)] = item;", "q->rear = (q->rear + 1) % MAX_QUEUE_SIZE; q->data[q->rear] = item;"),
                "empty-dequeue", reference.replace("exit(1);", "exit(0);"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("재귀 과제의 종료 조건·홀수 지수·막대 순서 오답을 구분합니다.")
    void execute_success_detectsRecursionMistakes() throws Exception {
        // given
        Map<String, String> exampleIds = Map.of(
                "recursion-factorial", "base-zero",
                "recursion-power", "odd-exponent",
                "recursion-fibonacci", "base-zero",
                "recursion-hanoi", "three-disks");

        for (Map.Entry<String, String> example : exampleIds.entrySet()) {
            Path contentDirectory = Path.of("../content", example.getKey(), "c");
            String reference = Files.readString(contentDirectory.resolve("reference.c"));
            String mistake = switch (example.getKey()) {
                case "recursion-factorial" -> reference.replace("if (n <= 1) return 1;", "if (n <= 1) return 0;");
                case "recursion-power" -> reference.replace("return x * power", "return power");
                case "recursion-fibonacci" -> reference.replace("if (n == 0) return 0;", "if (n == 0) return 1;");
                case "recursion-hanoi" -> reference.replace("hanoi_tower(n - 1, tmp, from, to);", "hanoi_tower(n - 1, tmp, to, from);");
                default -> throw new IllegalArgumentException(example.getKey());
            };
            JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(example.getValue())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")), List.of(new TestInput(example.getValue(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, result.tests().getFirst().status());
            Assertions.assertNotEquals(test.path("expected").path("stdout").asString(), result.tests().getFirst().stdout(), example.getKey());
        }
    }

    @Test
    @DisplayName("원형 큐의 포화 판정·순환 범위·조회 시 삭제 오답을 구분합니다.")
    void execute_success_detectsCircularQueueMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/queue-circular/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "full-after-wrap", reference.replace("(q->rear + 1) % MAX_QUEUE_SIZE == q->front", "q->rear == MAX_QUEUE_SIZE - 1"),
                "wrap-repeat", reference.replace("q->rear = (q->rear + 1) % MAX_QUEUE_SIZE;", "q->rear = (q->rear + 1) % (MAX_QUEUE_SIZE - 1);"),
                "peek-one", reference.replace("return q->data[(q->front + 1) % MAX_QUEUE_SIZE];", "q->front = (q->front + 1) % MAX_QUEUE_SIZE; return q->data[q->front];"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("덱의 앞 삽입·뒤 삭제·조회·포화 판정 오답을 구분합니다.")
    void execute_success_detectsDequeMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/deque-circular/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "add-front-one", reference.replace("(q->front - 1 + MAX_QUEUE_SIZE)", "(q->front + 1)"),
                "delete-rear-order", reference.replace("return q->data[previous];", "return q->data[q->rear];"),
                "get-front-preserves", reference.replace("return q->data[(q->front + 1) % MAX_QUEUE_SIZE];", "q->front = (q->front + 1) % MAX_QUEUE_SIZE; return q->data[q->front];"),
                "full-front-reverse", reference.replace("(q->rear + 1) % MAX_QUEUE_SIZE == q->front", "q->rear == MAX_QUEUE_SIZE - 1"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("연결 리스트의 연결 손실·잘못된 head·해제 누락 오답을 구분합니다.")
    void execute_success_detectsSinglyLinkedListMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/list-singly/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "insert-first-many", reference.replace("p->link = head;", "p->link = NULL;"),
                "insert-after-middle", reference.replace("pre->link = p;", "pre->link = p; head = p;"),
                "delete-after-tail", reference.replace("free(removed);\n    return head;\n}\n\nvoid print_list", "free(removed);\n    return pre;\n}\n\nvoid print_list"),
                "memory-first", reference.replace("free(removed);", "(void)removed;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertNotEquals(test.path("expected").path("stdout").asString(), actual.stdout(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("원형 리스트의 head 갱신·자기 연결·중복 출력 오답을 구분합니다.")
    void execute_success_detectsCircularListMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/list-circular/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "first-many", reference.replace("head->link = node;", "head->link = node; head = node;"),
                "last-many", reference.replace("head->link = node;\n        head = node;", "head->link = node;"),
                "one-self-last", reference.replace("node->link = head;", "node->link = NULL;"),
                "print-single", reference.replace("printf(\"\\n\");", "printf(\"%d->\\n\", head->data);"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("이중 리스트의 역방향 연결·헤드 보호·해제·출력 오답을 구분합니다.")
    void execute_success_detectsDoublyListMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/list-doubly/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "links-one", reference.replace("before->rlink->llink = node;", ""),
                "delete-middle", reference.replace("removed->rlink->llink = removed->llink;", ""),
                "head-preserves", reference.replace("if (removed == head) return;", "if (removed == head) removed = head->rlink;"),
                "memory-one", reference.replace("free(removed);", "(void)removed;"),
                "print-single", reference.replace("p != phead;", "p != phead && p->rlink != phead;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("연결 스택의 연결 손실·조회 시 삭제·해제 누락·공백 처리 오답을 구분합니다.")
    void execute_success_detectsLinkedStackMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/stack-linked/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "push-many", reference.replace("node->link = s->top;", "node->link = NULL;"),
                "peek-one", reference.replace("return s->top->data;", "return pop(s);"),
                "memory-one", reference.replace("free(removed);", "(void)removed;"),
                "empty-pop", reference.replace("exit(1);", "exit(0);"),
                "full-always-zero", reference.replace("return 0;", "return !is_empty(s);"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("연결 큐의 연결 손실·rear 초기화·해제 누락·공백 처리 오답을 구분합니다.")
    void execute_success_detectsLinkedQueueMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/queue-linked/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "enqueue-many", reference.replace("q->rear->link = node;", ""),
                "ends-last-delete", reference.replace("if (q->front == NULL) q->rear = NULL;", ""),
                "memory-one", reference.replace("free(removed);", "(void)removed;"),
                "empty-dequeue", reference.replace("exit(1);", "exit(0);"),
                "full-always-zero", reference.replace("return 0;", "return !is_empty(q);"),
                "print-many", reference.replace("QueueNode *p = q->front;", "QueueNode *p = q->rear;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("연결 덱의 역방향 링크·마지막 삭제·조회 시 삭제·해제 누락 오답을 구분합니다.")
    void execute_success_detectsLinkedDequeMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/deque-linked/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "add-front-many", reference.replace("else q->front->llink = node;", ""),
                "delete-front-links", reference.replace("else q->front->llink = NULL;", ""),
                "delete-rear-only", reference.replace("if (q->rear == NULL) q->front = NULL;", "if (q->rear == NULL) (void)q;"),
                "get-rear-preserves", reference.replace("return q->rear->data;", "return delete_rear(q);"),
                "memory-rear", reference.replace("free(removed);", "(void)removed;"),
                "empty-delete-front", reference.replace("exit(1);", "exit(0);"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("이진 트리의 값 손실·좌우 반전·자식 초기화·기존 노드 변경 오답을 구분합니다.")
    void execute_success_detectsBinaryTreeMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-binary/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "leaf-one", reference.replace("node->data = data;", "node->data = 0;"),
                "both-example", reference.replace("node->left = left;", "node->left = right;").replace("node->right = right;", "node->right = left;"),
                "left-one", reference.replace("node->left = left;", "node->left = NULL;"),
                "preserves-children", reference.replace("return node;", "if (left != NULL) left->data = -999; return node;"),
                "memory-parent", reference.replace("return node;", "if (left != NULL) malloc(sizeof(TreeNode)); return node;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("전위 순회의 루트 방문 지연·좌우 반전·누락·트리 변경 오답을 구분합니다.")
    void execute_success_detectsPreorderMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-preorder/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "root-example", reference.replace("printf(\"[%d] \", root->data);\n        preorder(root->left);", "preorder(root->left);\n        printf(\"[%d] \", root->data);"),
                "left-subtree", reference.replace("preorder(root->left);\n        preorder(root->right);", "preorder(root->right);\n        preorder(root->left);"),
                "sparse-right", reference.replace("preorder(root->right);", ""),
                "preserve-both", reference.replace("printf(\"[%d] \", root->data);", "printf(\"[%d] \", root->data); root->data = -999;"),
                "leaf-zero", reference.replace("if (root != NULL)", "if (root != NULL && root->data != 0)"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("중위 순회의 출력 위치·좌우 반전·노드 누락·트리 변경 오답을 구분합니다.")
    void execute_success_detectsInorderMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-inorder/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "root-example", reference.replace("inorder(root->left);\n        printf(\"[%d] \", root->data);", "printf(\"[%d] \", root->data);\n        inorder(root->left);"),
                "left-subtree", reference.replace("root->left", "root->temporary").replace("root->right", "root->left").replace("root->temporary", "root->right"),
                "sparse-right", reference.replace("inorder(root->right);", ""),
                "preserve-both", reference.replace("printf(\"[%d] \", root->data);", "printf(\"[%d] \", root->data); root->data = -999;"),
                "leaf-zero", reference.replace("if (root != NULL)", "if (root != NULL && root->data != 0)"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("후위 순회의 부모 조기 방문·좌우 반전·노드 누락·트리 변경 오답을 구분합니다.")
    void execute_success_detectsPostorderMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-postorder/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "root-example", reference.replace("postorder(root->right);\n        printf(\"[%d] \", root->data);", "printf(\"[%d] \", root->data);\n        postorder(root->right);"),
                "left-subtree", reference.replace("postorder(root->left);\n        postorder(root->right);", "postorder(root->right);\n        postorder(root->left);"),
                "sparse-right", reference.replace("postorder(root->right);", ""),
                "preserve-both", reference.replace("printf(\"[%d] \", root->data);", "printf(\"[%d] \", root->data); root->data = -999;"),
                "leaf-zero", reference.replace("if (root != NULL)", "if (root != NULL && root->data != 0)"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            JsonNode test = tests.valueStream().filter(item -> item.path("id").asString().equals(mistake.getKey())).findFirst().orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("레벨 순회의 깊이·좌우 순서·누락·트리 변경 오답을 구분합니다.")
    void execute_success_detectsLevelOrderMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-level-order/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "root-example", reference.replace("TreeNode *node = dequeue(&q);", "TreeNode *node = q.data[q.rear--];"),
                "left-subtree", reference.replace("if (node->left != NULL) enqueue(&q, node->left);\n        if (node->right != NULL) enqueue(&q, node->right);", "if (node->right != NULL) enqueue(&q, node->right);\n        if (node->left != NULL) enqueue(&q, node->left);"),
                "sparse-right", reference.replace("if (node->right != NULL) enqueue(&q, node->right);", ""),
                "preserve-both", reference.replace("printf(\"[%d] \", node->data);", "printf(\"[%d] \", node->data); node->left = NULL;"),
                "leaf-zero", reference.replace("if (root == NULL) return;", "if (root == NULL || root->data == 0) return;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("이진 탐색 트리의 탐색·중복·삭제 연결·후계자·해제 오답을 구분합니다.")
    void execute_success_detectsBstMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/tree-bst/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "search-branches", reference.replace("if (key < node->key) return search(node->left, key);", "if (key < node->key) return search(node->right, key);"),
                "insert-order", reference.replace("node->left = insert_node(node->left, key);", "insert_node(node->left, key);"),
                "duplicate-root", reference.replace("else if (key > node->key) node->right = insert_node(node->right, key);", "else node->right = insert_node(node->right, key);"),
                "minimum-deep", reference.replace("while (current->left != NULL) current = current->left;", "if (current->left != NULL) current = current->left;"),
                "delete-root-left", reference.replace("TreeNode *next = root->left;", "TreeNode *next = NULL;"),
                "delete-successor-deep", reference.replace("root->right = delete_node(root->right, next->key);", "root->right = NULL;"),
                "memory-release", reference.replace("free(root);", ""),
                "search-preserve", reference.replace("if (key == node->key) return node;", "if (key == node->key) { node->left = NULL; return node; }"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"max", "min"})
    @DisplayName("힙의 비교·부모 이동·자식 선택·크기·오류 종료 오답을 구분합니다.")
    void execute_success_detectsHeapMistakes(String mode) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/heap-" + mode + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        String comparison = mode.equals("max") ? ">" : "<";
        String reversed = mode.equals("max") ? "<" : ">";
        Map<String, String> mistakes = Map.of(
                "insert-example", reference.replace("item.key " + comparison + " h->heap[i / 2].key", "item.key " + reversed + " h->heap[i / 2].key"),
                "insert-rise", reference.replace("i /= 2;", "i = 1;"),
                "child-right", reference.replace("if (child < h->heap_size &&", "if (0 &&"),
                "child-left-only", reference.replace("while (child <= h->heap_size)", "while (child < h->heap_size)"),
                "delete-size", reference.replace("h->heap[(h->heap_size)--]", "h->heap[h->heap_size]"),
                "init-empty", reference.replace("h->heap_size = 0;", "h->heap_size = 1;"),
                "full-boundary", reference.replace("h->heap_size == MAX_ELEMENT - 1", "h->heap_size == MAX_ELEMENT - 2"),
                "empty-initial", reference.replace("exit(1);", "exit(0);"),
                "zero-negative", reference.replace("h->heap[i] = item;", ""));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mode + ":" + mistake.getKey());
        }

        // given
        String alternative = reference.replace("item.key " + comparison + " h->heap[i / 2].key", "item.key " + comparison + "= h->heap[i / 2].key")
                .replace("h->heap[child].key " + reversed + " h->heap[child + 1].key", "h->heap[child].key " + reversed + "= h->heap[child + 1].key");
        JsonNode duplicate = tests.valueStream()
                .filter(test -> test.path("id").asString().equals("duplicate"))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, harness, List.of(new TestInput("duplicate", duplicate.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        TestExecutionResult actual = result.tests().getFirst();
        Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
        Assertions.assertEquals(duplicate.path("expected").path("stdout").asString(), actual.stdout());
        Assertions.assertEquals(0, actual.exitCode());
        Assertions.assertTrue(actual.stderr().isBlank());
    }

    @Test
    @DisplayName("체이닝 해싱의 음수 버킷·충돌·중복·검색 주소·삭제 연결·할당 오류를 구분합니다.")
    void execute_success_detectsChainingMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/hash-chaining/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "hash-negative", reference.replace("return (key % TABLE_SIZE + TABLE_SIZE) % TABLE_SIZE;", "return key % TABLE_SIZE;"),
                "insert-collisions", reference.replace("else before->link = created;", "else ht[hash_value] = created;"),
                "duplicate-deep", reference.replace("if (node->item.key == item.key) return;", ""),
                "search-chain", reference.replace("if (node->item.key == item.key) return node;", "if (node->item.key == item.key) return ht[hash_value];"),
                "delete-head", reference.replace("if (before == NULL) ht[hash_value] = node->link;", "if (before == NULL) ht[hash_value] = NULL;"),
                "delete-middle", reference.replace("else before->link = node->link;", "else before->link = NULL;"),
                "memory-delete", reference.replace("free(node);", ""),
                "allocation-empty", reference.replace("exit(1);", "exit(0);"),
                "search-preserve", reference.replace("if (node->item.key == item.key) return node;", "if (node->item.key == item.key) { node->link = NULL; return node; }"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }
    }

    @Test
    @DisplayName("유니온 파인드의 대표·합치기 방향·순환·부분 압축·다른 경로 변경 오답을 구분합니다.")
    void execute_success_detectsUnionFindMistakes() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/set-union-find/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        Map<String, String> mistakes = Map.of(
                "init-single", reference.replace("i < n;", "i < n - 1;"),
                "find-roots", reference.replace("if (parent[curr] == -1) return curr;", "if (parent[curr] == -1) return -1;"),
                "find-deep", reference.replace("return parent[curr] = set_find(parent[curr]);", "return parent[curr];"),
                "compress-all", reference.replace("return parent[curr] = set_find(parent[curr]);", "return set_find(parent[curr]);"),
                "compress-middle", reference.replace("return parent[curr] = set_find(parent[curr]);", "int root = curr; while (parent[root] != -1) root = parent[root]; parent[curr] = root; return root;"),
                "union-nonroots", reference.replace("if (root1 != root2) parent[root1] = root2;", "if (root1 != root2) parent[a] = b;"),
                "union-direction", reference.replace("parent[root1] = root2;", "parent[root2] = root1;"),
                "same-self", reference.replace("if (root1 != root2) parent[root1] = root2;", "parent[root1] = root2;"),
                "preserve-branch", reference.replace("return parent[curr] = set_find(parent[curr]);", "int root = set_find(parent[curr]); for (int i = 0; i < MAX_VERTICES; i++) { if (parent[i] == curr) parent[i] = root; } return parent[curr] = root;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mistake.getKey());
        }

        // given
        String iterative = reference.replace("return parent[curr] = set_find(parent[curr]);", "int root = curr; while (parent[root] != -1) root = parent[root]; while (parent[curr] != -1) { int next = parent[curr]; parent[curr] = root; curr = next; } return root;");
        List<TestInput> inputs = tests.valueStream()
                .map(test -> new TestInput(test.path("id").asString(), test.path("input").asString()))
                .toList();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, iterative, harness, inputs);

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        Assertions.assertEquals(tests.size(), result.tests().size());
        for (int index = 0; index < tests.size(); index++) {
            TestExecutionResult actual = result.tests().get(index);
            JsonNode expected = tests.get(index).path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertEquals(expected.path("stdout").asString(), actual.stdout(), actual.testId());
            Assertions.assertEquals(expected.path("exitCode").asInt(), actual.exitCode(), actual.testId());
            Assertions.assertEquals(expected.path("stderrRequired").asBoolean(), !actual.stderr().isBlank(), actual.testId());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"iterative", "recursive"})
    @DisplayName("이진 탐색의 구간·반환 인덱스·배열 변경·결과 전달 오답을 구분합니다.")
    void execute_success_detectsBinarySearchMistakes(String mode) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/search-binary-" + mode + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String harness = Files.readString(contentDirectory.resolve("harness.c"));
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        boolean iterative = mode.equals("iterative");
        Map<String, String> mistakes = Map.of(
                "single-found", reference.replace(iterative ? "while (low <= high)" : "if (low > high)", iterative ? "while (low < high)" : "if (low >= high)"),
                "search-example", reference.replace("return middle;", "return list[middle];"),
                "search-left", reference.replace(iterative ? "key > list[middle]" : "key < list[middle]", iterative ? "key < list[middle]" : "key > list[middle]"),
                "search-right", reference.replace("middle + 1", "middle + 2"),
                "subrange-found", reference.replace("return middle;", "return middle - low;"),
                "missing-between", reference.replace("return -1;", "return 0;"),
                "preserve-array", reference.replace("if (key == list[middle]) return middle;", "if (key == list[middle]) { list[middle] = 999; return middle; }"),
                "subrange-excluded", reference.replace("int search_binary(int key, int low, int high)\n{\n", "int search_binary(int key, int low, int high)\n{\n    if (key == list[0]) return 0;\n"),
                "max-array", reference.replace(iterative ? "while (low <= high)" : "return search_binary(key, middle + 1, high);", iterative ? "if (low <= high)" : "search_binary(key, middle + 1, high); return -1;"));

        for (Map.Entry<String, String> mistake : mistakes.entrySet()) {
            Assertions.assertNotEquals(reference, mistake.getValue(), mistake.getKey());
            JsonNode test = tests.valueStream()
                    .filter(item -> item.path("id").asString().equals(mistake.getKey()))
                    .findFirst()
                    .orElseThrow();
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake.getValue(), harness, List.of(new TestInput(mistake.getKey(), test.path("input").asString())));

            // when
            ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

            // then
            Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
            TestExecutionResult actual = result.tests().getFirst();
            JsonNode expected = test.path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                    && expected.path("exitCode").asInt() == actual.exitCode()
                    && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), mode + ":" + mistake.getKey());
        }

        // given
        String alternative = reference.replace("if (key == list[middle]) return middle;", "if (key == list[middle]) { while (middle < high && list[middle + 1] == key) middle++; return middle; }");
        List<JsonNode> duplicateTests = tests.valueStream()
                .filter(test -> test.path("id").asString().equals("duplicates") || test.path("id").asString().equals("all-equal"))
                .toList();
        List<TestInput> inputs = duplicateTests.stream()
                .map(test -> new TestInput(test.path("id").asString(), test.path("input").asString()))
                .toList();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, harness, inputs);

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        Assertions.assertEquals(duplicateTests.size(), result.tests().size());
        for (int index = 0; index < duplicateTests.size(); index++) {
            TestExecutionResult actual = result.tests().get(index);
            JsonNode expected = duplicateTests.get(index).path("expected");
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertEquals(expected.path("stdout").asString(), actual.stdout());
            Assertions.assertEquals(0, actual.exitCode());
            Assertions.assertTrue(actual.stderr().isBlank());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"selection", "bubble", "insertion", "merge", "quick", "heap"})
    @DisplayName("정렬의 반복 범위·값 손실·재귀 누락·힙 복원 오답을 구분합니다.")
    void execute_success_detectsSortMistakes(String algorithm) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/sort-" + algorithm + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (algorithm) {
            case "selection" -> reference.replace("i < n - 1", "i < n - 2");
            case "bubble" -> reference.replace("i < n - 1", "i < 1");
            case "insertion" -> reference.replace("list[j + 1] = key;", "list[j + 1] = key + 1;");
            case "merge" -> reference.replace("sorted[k++] = list[j++];", "j++;");
            case "quick" -> reference.replace("quick_sort(list, left, q - 1);", "(void)left;");
            case "heap" -> reference.replace("list[child] < list[child + 1]", "list[child] > list[child + 1]");
            default -> throw new IllegalArgumentException(algorithm);
        };
        Assertions.assertNotEquals(reference, mistake);
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals("sort-example"))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput("sort-example", test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), algorithm);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matrix", "list", "dfs", "bfs"})
    @DisplayName("그래프의 역방향 간선·노드 해제·방문 순서·FIFO 오답을 구분합니다.")
    void execute_success_detectsGraphMistakes(String implementation) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/graph-" + implementation + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (implementation) {
            case "matrix" -> reference.replace("g->adj_mat[v][u] = 1;", "g->adj_mat[v][u] = 0;");
            case "list" -> reference.replace("free(p);", "(void)p;");
            case "dfs" -> reference.replace("for (int w = 0; w < g->n; w++)", "for (int w = g->n - 1; w >= 0; w--)");
            case "bfs" -> reference.replace("return q->data[q->front];", "return q->data[q->rear];");
            default -> throw new IllegalArgumentException(implementation);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (implementation) {
            case "list" -> "free-nodes";
            case "bfs" -> "branch-depth";
            default -> "graph-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), implementation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kruskal-cycle", "kruskal-cost", "prim-path", "prim-zero", "prim-disconnected"})
    @DisplayName("최소 신장 트리의 사이클·최소 비용·0 가중치·연결 실패 오답을 구분합니다.")
    void execute_success_detectsMstMistakes(String scenario) throws Exception {
        // given
        String algorithm = scenario.startsWith("kruskal") ? "kruskal" : "prim";
        Path contentDirectory = Path.of("../content/graph-" + algorithm + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "kruskal-cycle" -> reference.replace("parent[start_root] = end_root;", "(void)end_root;");
            case "kruskal-cost" -> reference.replace("(a->weight > b->weight) - (a->weight < b->weight)", "(a->weight < b->weight) - (a->weight > b->weight)");
            case "prim-path" -> reference.replace("distance[next] = g->weight[vertex][next];", "distance[next] = distance[vertex] + g->weight[vertex][next];");
            case "prim-zero" -> reference.replace("!selected[next] && g->weight", "!selected[next] && g->weight[vertex][next] != 0 && g->weight");
            case "prim-disconnected" -> reference.replace("distance[vertex] == INF) return -1;", "distance[vertex] == INF) return count;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "kruskal-cycle" -> "cycle-choice";
            case "prim-path" -> "edge-key-not-path";
            case "prim-zero" -> "zero-weight";
            case "prim-disconnected" -> "isolated-vertex";
            default -> "weighted-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kruskal", "prim"})
    @DisplayName("동률에서 다른 간선을 선택한 최소 신장 트리도 허용합니다.")
    void execute_success_withAlternativeMst(String algorithm) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/graph-" + algorithm + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String alternative;
        if (algorithm.equals("kruskal")) {
            alternative = reference.replace("return (a->weight > b->weight) - (a->weight < b->weight);",
                    "if (a->weight == b->weight) return b->start - a->start; return (a->weight > b->weight) - (a->weight < b->weight);");
        } else {
            alternative = reference.replace("distance[i] < distance[vertex]", "distance[i] <= distance[vertex]")
                    .replace("g->weight[vertex][next] < distance[next]", "g->weight[vertex][next] != INF && g->weight[vertex][next] <= distance[next]");
        }
        Assertions.assertNotEquals(reference, alternative);
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals("equal-weights"))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput("equal-weights", test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
        Assertions.assertEquals(test.path("expected").path("stdout").asString(), actual.stdout());
        Assertions.assertEquals(0, actual.exitCode());
        Assertions.assertTrue(actual.stderr().isBlank());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dijkstra-edge", "dijkstra-zero", "bellman-passes", "bellman-infinity", "bellman-cycle", "bellman-unreachable-cycle"})
    @DisplayName("최단 경로의 누적 거리·0 가중치·완화 횟수·음수 사이클 판정 오답을 구분합니다.")
    void execute_success_detectsShortestPathMistakes(String scenario) throws Exception {
        // given
        String algorithm = scenario.startsWith("dijkstra") ? "dijkstra" : "bellman-ford";
        Path contentDirectory = Path.of("../content/graph-" + algorithm + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "dijkstra-edge" -> reference.replace("distance[next] = distance[vertex] + g->weight[vertex][next];", "distance[next] = g->weight[vertex][next];");
            case "dijkstra-zero" -> reference.replace("g->weight[vertex][next] != INF", "g->weight[vertex][next] != INF && g->weight[vertex][next] != 0");
            case "bellman-passes" -> reference.replace("pass < g->n - 1", "pass < 1");
            case "bellman-infinity", "bellman-unreachable-cycle" -> reference.replace("distance[edge.from] != INF", "1");
            case "bellman-cycle" -> reference.replace("return 0;", "return 1;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "dijkstra-edge" -> "edge-not-total";
            case "dijkstra-zero" -> "zero-weight";
            case "bellman-passes" -> "reverse-chain";
            case "bellman-infinity" -> "separate-components";
            case "bellman-cycle" -> "reachable-negative-cycle";
            case "bellman-unreachable-cycle" -> "unreachable-negative-cycle";
            default -> throw new IllegalArgumentException(scenario);
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"loop-order", "infinity-left", "infinity-right", "isolated-negative-cycle", "initialization"})
    @DisplayName("플로이드–워셜의 반복 순서·INF 처리·음수 사이클·초기화 오답을 구분합니다.")
    void execute_success_detectsFloydWarshallMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/graph-floyd-warshall/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "loop-order" -> reference.replace("for (int k = 0; k < g->n; k++) {\n        for (int i = 0; i < g->n; i++) {",
                    "for (int i = 0; i < g->n; i++) {\n        for (int k = 0; k < g->n; k++) {");
            case "infinity-left" -> reference.replace("distance[i][k] != INF", "1");
            case "infinity-right" -> reference.replace("distance[k][j] != INF", "1");
            case "isolated-negative-cycle" -> reference.replace("return 0;", "return 1;");
            case "initialization" -> reference.replace("distance[i][j] = g->weight[i][j];", "distance[i][j] = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = scenario.equals("initialization") ? "all-pairs-example" : scenario;
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"direction", "decrement", "early-enqueue", "partial-cycle", "missing-vertex"})
    @DisplayName("위상 정렬의 진입 차수 방향·감소·조기 삽입·부분 사이클·정점 누락 오답을 구분합니다.")
    void execute_success_detectsTopologicalMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/graph-topological/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "direction" -> reference.replace("in_degree[to]++;", "in_degree[from]++;");
            case "decrement" -> reference.replace("in_degree[next]--;", "(void)next;");
            case "early-enqueue" -> reference.replace("if (in_degree[next] == 0)", "if (in_degree[next] >= 0)");
            case "partial-cycle" -> reference.replace("if (count != g->n) return -1;", "if (count == 0) return -1;");
            case "missing-vertex" -> reference.replace("order[count++] = vertex;", "order[count++] = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "direction" -> "reverse-chain";
            case "partial-cycle" -> "source-before-cycle";
            default -> "diamond";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @Test
    @DisplayName("기준 코드와 다른 유효한 위상 정렬 순서도 허용합니다.")
    void execute_success_withAlternativeTopologicalOrder() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/graph-topological/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String alternative = reference.replace("for (int v = 0; v < g->n; v++)", "for (int v = g->n - 1; v >= 0; v--)")
                .replace("for (int next = 0; next < g->n; next++)", "for (int next = g->n - 1; next >= 0; next--)");
        Assertions.assertNotEquals(reference, alternative);
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        List<TestInput> inputs = new ArrayList<>();
        tests.forEach(test -> inputs.add(new TestInput(test.path("id").asString(), test.path("input").asString())));
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, Files.readString(contentDirectory.resolve("harness.c")), inputs);

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        Assertions.assertEquals(tests.size(), result.tests().size());
        for (int index = 0; index < tests.size(); index++) {
            TestExecutionResult actual = result.tests().get(index);
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertEquals(tests.get(index).path("expected").path("stdout").asString(), actual.stdout(), actual.testId());
            Assertions.assertEquals(0, actual.exitCode(), actual.testId());
            Assertions.assertTrue(actual.stderr().isBlank(), actual.testId());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"search-end", "search-empty", "split-delimiter", "join-terminator", "reverse-terminator", "sort-order", "trie-prefix", "trie-shared", "trie-free"})
    @DisplayName("문자열 경계·널 종료·정렬과 트라이 접두사·공유 경로·노드 해제 오답을 구분합니다.")
    void execute_success_detectsStringAndTrieMistakes(String scenario) throws Exception {
        // given
        String exerciseId;
        if (scenario.startsWith("search")) {
            exerciseId = "string-brute-force";
        } else if (scenario.startsWith("trie")) {
            exerciseId = "trie-basic";
        } else {
            exerciseId = "string-transform";
        }
        Path contentDirectory = Path.of("../content/" + exerciseId + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "search-end" -> reference.replace("start <= n - m", "start < n - m");
            case "search-empty" -> reference.replace("int m = (int)strlen(pattern);", "int m = (int)strlen(pattern); if (m == 0) return -1;");
            case "split-delimiter" -> reference.replace("while (text[index] == delimiter)", "if (text[index] == delimiter)");
            case "join-terminator" -> reference.replace("result[length] = '\\0';", "(void)length;");
            case "reverse-terminator" -> reference.replace("(int)strlen(text) - 1", "(int)strlen(text)");
            case "sort-order" -> reference.replace("strcmp(words[j], words[j + 1]) > 0", "strcmp(words[j], words[j + 1]) < 0");
            case "trie-prefix" -> reference.replace("return node->is_end;", "return 1;");
            case "trie-shared" -> reference.replace("!child->is_end && !has_children(child)", "!child->is_end");
            case "trie-free" -> reference.replace("free(child);", "(void)child;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "search-end" -> "at-end";
            case "search-empty" -> "empty-pattern";
            case "split-delimiter" -> "split-example";
            case "join-terminator" -> "join-example";
            case "reverse-terminator" -> "reverse-odd";
            case "sort-order" -> "sort-example";
            case "trie-prefix" -> "prefix-example";
            case "trie-shared" -> "delete-prefix";
            case "trie-free" -> "free-leaf";
            default -> throw new IllegalArgumentException(scenario);
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"permutation-duplicate", "permutation-restore", "combination-start", "combination-empty", "subset-branch", "subset-restore"})
    @DisplayName("완전 탐색의 원소 재사용·상태 복구·조합 중복·빈 선택·분기 누락 오답을 구분합니다.")
    void execute_success_detectsEnumerationMistakes(String scenario) throws Exception {
        // given
        String kind;
        if (scenario.startsWith("permutation")) {
            kind = "permutations";
        } else if (scenario.startsWith("combination")) {
            kind = "combinations";
        } else {
            kind = "subsets";
        }
        Path contentDirectory = Path.of("../content/exhaustive-" + kind + "/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "permutation-duplicate" -> reference.replace("if (used[value]) continue;", "(void)used;");
            case "permutation-restore" -> reference.replace("used[value] = 0;", "used[value] = 1;");
            case "combination-start" -> reference.replace("value + 1, depth + 1", "value, depth + 1");
            case "combination-empty" -> reference.replace("if (depth == r) {", "if (depth == r) { if (r == 0) return;");
            case "subset-branch" -> reference.replace("selected[index] = 1;", "selected[index] = 0;");
            case "subset-restore" -> reference.replace("selected[index] = 0;\n}", "selected[index] = 1;\n}");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = scenario.equals("combination-empty") ? "empty-choice" : "generation-example";
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"column", "diagonal", "base", "pruning", "restore", "sum"})
    @DisplayName("N-Queen의 열·대각선 검사·종료 조건·가지치기·상태 복구·합산 오답을 구분합니다.")
    void execute_success_detectsNQueenMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/backtracking-nqueen/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "column" -> reference.replace("columns[previous] == column", "0");
            case "diagonal" -> reference.replace("abs(columns[previous] - column) == row - previous", "0");
            case "base" -> reference.replace("if (row == n) return 1;", "if (row == n) return 0;");
            case "pruning" -> reference.replace("if (!is_safe(columns, row, column)) continue;", "(void)row;");
            case "restore" -> reference.replace("columns[row] = -1;", "columns[row] = 0;");
            case "sum" -> reference.replace("count += solve_n_queens", "count = solve_n_queens");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "column" -> "same-column";
            case "diagonal" -> "left-diagonal";
            default -> "nqueen-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unsorted", "start", "duration", "endpoint", "negative", "identity", "last-end"})
    @DisplayName("활동 선택의 정렬·그리디 기준·경계 시각·음수 시각·원래 ID·종료 갱신 오답을 구분합니다.")
    void execute_success_detectsActivitySelectionMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/greedy-activity-selection/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "unsorted" -> reference.replace("qsort(activities, n, sizeof(Activity), compare_activities);", "(void)compare_activities;");
            case "start" -> reference.replace("a->end", "a->start").replace("b->end", "b->start");
            case "duration" -> reference.replace("(a->end > b->end) - (a->end < b->end)",
                    "(a->end - a->start > b->end - b->start) - (a->end - a->start < b->end - b->start)");
            case "endpoint" -> reference.replace("activities[i].start >= last_end", "activities[i].start > last_end");
            case "negative" -> reference.replace("count == 0 || ", "");
            case "identity" -> reference.replace("selected[count++] = activities[i].id;", "selected[count++] = i;");
            case "last-end" -> reference.replace("last_end = activities[i].end;", "last_end = activities[i].start;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "unsorted", "identity" -> "original-ids";
            case "start" -> "earliest-start-trap";
            case "duration" -> "shortest-duration-trap";
            case "endpoint" -> "touching-endpoints";
            case "negative" -> "negative-times";
            case "last-end" -> "overlap-by-one";
            default -> throw new IllegalArgumentException(scenario);
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && expected.path("exitCode").asInt() == actual.exitCode()
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @Test
    @DisplayName("동률에서 다른 활동 ID를 선택한 최적해도 허용합니다.")
    void execute_success_withAlternativeActivitySelection() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/greedy-activity-selection/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String alternative = reference.replace("return a->id - b->id;", "return b->id - a->id;");
        Assertions.assertNotEquals(reference, alternative);
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        List<JsonNode> tiedTests = tests.valueStream()
                .filter(test -> test.path("criterionId").asString().equals("ties"))
                .toList();
        Assertions.assertFalse(tiedTests.isEmpty());
        List<TestInput> inputs = new ArrayList<>();
        tiedTests.forEach(test -> inputs.add(new TestInput(test.path("id").asString(), test.path("input").asString())));
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, Files.readString(contentDirectory.resolve("harness.c")), inputs);

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        Assertions.assertEquals(tiedTests.size(), result.tests().size());
        for (int index = 0; index < tiedTests.size(); index++) {
            TestExecutionResult actual = result.tests().get(index);
            Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
            Assertions.assertEquals(tiedTests.get(index).path("expected").path("stdout").asString(), actual.stdout());
            Assertions.assertEquals(0, actual.exitCode());
            Assertions.assertTrue(actual.stderr().isBlank());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memo-store", "memo-reuse", "table-base", "table-zero-range", "table-loop", "table-preserve"})
    @DisplayName("DP 피보나치의 저장·캐시 재사용·기저값·반복 범위·표 보존 오답을 구분합니다.")
    void execute_success_detectsDpFibonacciMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/dp-fibonacci/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "memo-store" -> reference.replace("memo[n] = previous + before_previous;", "return previous + before_previous;");
            case "memo-reuse" -> reference.replace("if (memo[n] != -1)", "if (n < 2)");
            case "table-base" -> reference.replace("table[0] = 0;", "table[0] = 1;");
            case "table-zero-range" -> reference.replace("if (n >= 1)", "if (n >= 0)");
            case "table-loop" -> reference.replace("i <= n", "i < n");
            case "table-preserve" -> reference.replace("table[0] = 0;", "for (int i = 0; i <= MAX_N; i++) table[i] = -1; table[0] = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "memo-reuse" -> "cached-result";
            case "table-base", "table-zero-range" -> "zero-example";
            case "table-preserve" -> "large-then-small";
            default -> "fibonacci-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(actual.exitCode())
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reuse", "include-only", "exact-fit", "missing-item", "base", "preserve"})
    @DisplayName("0/1 배낭의 중복 사용·최대 선택·경계·기저값·표 보존 오답을 구분합니다.")
    void execute_success_detectsKnapsackMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/dp-knapsack/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "reuse" -> reference.replace("table[i - 1][w - items[i - 1].weight]", "table[i][w - items[i - 1].weight]");
            case "include-only" -> reference.replace("if (included > best)", "if (1)");
            case "exact-fit" -> reference.replace("items[i - 1].weight <= w", "items[i - 1].weight < w");
            case "missing-item" -> reference.replace("i <= n", "i < n");
            case "base" -> reference.replace("table[0][w] = 0;", "table[0][w] = 1;");
            case "preserve" -> reference.replace("// 물건이 없는 기저 행 초기화", "for (int i = 0; i <= MAX_ITEMS; i++) for (int w = 0; w <= MAX_CAPACITY; w++) table[i][w] = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "reuse" -> "no-reuse";
            case "include-only" -> "skip-better";
            case "exact-fit" -> "exact-fit";
            case "base" -> "empty-example";
            case "preserve" -> "smaller-then-larger";
            default -> "knapsack-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(actual.exitCode())
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"substring", "diagonal", "up-only", "left-only", "base", "loop", "preserve"})
    @DisplayName("LCS의 부분 문자열·대각선·위·왼쪽·기저값·계산 범위 오답을 구분합니다.")
    void execute_success_detectsLcsMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/dp-lcs/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "substring" -> reference.replace("table[i][j] = best;", "table[i][j] = 0;");
            case "diagonal" -> reference.replace("table[i - 1][j - 1] + 1", "table[i - 1][j] + 1");
            case "up-only" -> reference.replace("if (table[i][j - 1] > best)", "if (0)");
            case "left-only" -> reference.replace("int best = table[i - 1][j];", "int best = table[i][j - 1];");
            case "base" -> reference.replace("table[0][j] = 0;", "table[0][j] = 1;");
            case "loop" -> reference.replace("j <= n", "j < n");
            case "preserve" -> reference.replace("// 빈 접두사의 기저 행·열 초기화", "for (int i = 0; i <= MAX_LENGTH; i++) for (int j = 0; j <= MAX_LENGTH; j++) table[i][j] = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "substring" -> "no-reset";
            case "diagonal" -> "repeated";
            case "up-only" -> "left-better";
            case "left-only" -> "up-better";
            case "base" -> "empty-first";
            case "preserve" -> "large-then-small";
            default -> "lcs-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(actual.exitCode())
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reuse", "move-left", "move-right", "adjacent", "values", "missing", "mutate"})
    @DisplayName("투 포인터의 같은 위치 재사용·이동 방향·인접 쌍·반환값·입력 변경 오답을 구분합니다.")
    void execute_success_detectsTwoPointerMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/two-pointer-pair/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "reuse" -> reference.replace("left < right", "left <= right");
            case "move-left" -> reference.replace("left++;", "right--;");
            case "move-right" -> reference.replace("right--;", "left++;");
            case "adjacent" -> reference.replace("left < right", "left + 1 < right");
            case "values" -> reference.replace("(Pair){left, right}", "(Pair){values[left], values[right]}");
            case "missing" -> reference.replace("(Pair){-1, -1}", "(Pair){0, 0}");
            case "mutate" -> reference.replace("int left = 0;", "((int *)values)[0] = 99; int left = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "reuse" -> "single-no-reuse";
            case "move-left" -> "move-left";
            case "move-right" -> "move-right";
            case "adjacent" -> "adjacent";
            case "missing" -> "absent-example";
            default -> "pair-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(actual.exitCode())
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @Test
    @DisplayName("다른 유효한 인덱스 쌍을 반환하는 구현도 허용합니다.")
    void execute_success_withAlternativePair() throws Exception {
        // given
        Path contentDirectory = Path.of("../content/two-pointer-pair/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String alternative = reference.substring(0, reference.indexOf("Pair two_sum(")) + """
                Pair two_sum(const int values[], int n, int target)
                {
                    for (int right = 1; right < n; right++) {
                        for (int left = 0; left < right; left++) {
                            if (values[left] + values[right] == target) return (Pair){left, right};
                        }
                    }
                    return (Pair){-1, -1};
                }
                """;
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals("many-solutions"))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, alternative, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput("many-solutions", test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        Assertions.assertEquals(TestExecutionResult.Status.COMPLETED, actual.status());
        Assertions.assertEquals(test.path("expected").path("stdout").asString(), actual.stdout());
        Assertions.assertEquals(0, actual.exitCode());
        Assertions.assertTrue(actual.stderr().isBlank());
    }

    @ParameterizedTest
    @ValueSource(strings = {"zero-best", "outgoing", "incoming", "last-window", "length", "overwrite", "mutate"})
    @DisplayName("슬라이딩 윈도의 음수 초기화·교체 인덱스·마지막 구간·고정 길이·입력 변경 오답을 구분합니다.")
    void execute_success_detectsSlidingWindowMistakes(String scenario) throws Exception {
        // given
        Path contentDirectory = Path.of("../content/sliding-window-max-sum/c");
        String reference = Files.readString(contentDirectory.resolve("reference.c"));
        String mistake = switch (scenario) {
            case "zero-best" -> reference.replace("int best = sum;", "int best = 0;");
            case "outgoing" -> reference.replace("values[i - k]", "values[i - k + 1]");
            case "incoming" -> reference.replace("+ values[i];", "+ values[i - 1];");
            case "last-window" -> reference.replace("i < n", "i < n - 1");
            case "length" -> reference.replace("i < k", "i <= k");
            case "overwrite" -> reference.replace("if (sum > best)", "if (1)");
            case "mutate" -> reference.replace("int sum = 0;", "((int *)values)[0] = 99; int sum = 0;");
            default -> throw new IllegalArgumentException(scenario);
        };
        Assertions.assertNotEquals(reference, mistake);
        String testId = switch (scenario) {
            case "zero-best" -> "negative-example";
            case "outgoing" -> "outgoing";
            case "incoming" -> "incoming";
            case "last-window" -> "last-best";
            case "overwrite" -> "first-best";
            default -> "window-example";
        };
        JsonNode tests = JsonMapper.builder().build().readTree(Files.readString(contentDirectory.resolve("tests.json")));
        JsonNode test = tests.valueStream()
                .filter(item -> item.path("id").asString().equals(testId))
                .findFirst()
                .orElseThrow();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, mistake, Files.readString(contentDirectory.resolve("harness.c")),
                List.of(new TestInput(testId, test.path("input").asString())));

        // when
        ExecutionResult result = fixture.codeExecutionService.execute(request, actual -> {});

        // then
        Assertions.assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status(), result.preparation().toString());
        TestExecutionResult actual = result.tests().getFirst();
        JsonNode expected = test.path("expected");
        Assertions.assertFalse(expected.path("stdout").asString().equals(actual.stdout())
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(actual.exitCode())
                && expected.path("stderrRequired").asBoolean() == !actual.stderr().isBlank(), scenario);
    }

    @AfterEach
    void leavesNoContainersOrWorkspaces() throws Exception {
        fixture.assertCleanedUp();
    }
}
