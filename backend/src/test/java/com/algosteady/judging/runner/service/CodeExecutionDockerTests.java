package com.algosteady.judging.runner.service;

import com.algosteady.judging.runner.config.RunnerProperties;
import com.algosteady.judging.runner.infrastructure.DockerContainerManager;
import com.algosteady.judging.runner.infrastructure.ProcessExecutor;
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
import org.springframework.core.io.Resource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Docker 기반 C 실행·제한·자원 정리 검증 */
@EnabledIfSystemProperty(named = "algosteady.docker.tests", matches = "true")
class CodeExecutionDockerTests {

    private final RunnerProperties properties = new RunnerProperties(Map.of("c", "algosteady-runner-c:14.4.0"),
            Duration.ofSeconds(30), Duration.ofSeconds(2), Duration.ofSeconds(10), 1048576, 1, "512m", 128);
    private final ProcessExecutor executor = new ProcessExecutor();
    private final CodeExecutionService service = new CodeExecutionService(new DockerContainerManager(executor, properties));
    private final String mainHarness = "#include \"submission.c\"\n";
    private final Set<Path> initialWorkspaces = workspaces();
    private final String containerPrefix = "algosteady-runner-";

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
        ExecutionResult result = service.execute(request, delivered::add);

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
            ExecutionResult result = service.execute(request, actual -> {});

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
    @DisplayName("컴파일 실패 시 테스트를 실행하지 않습니다.")
    void compilationFailureDoesNotExecuteTests() throws Exception {
        // given
        // when
        ExecutionResult result = service.execute(new ExecutionRequest(LanguageSpec.C, "invalid C code;", mainHarness,
                List.of(new TestInput("example", ""))), test -> {});

        // then
        assertEquals(PreparationResult.Status.FAILED, result.preparation().status());
        assertTrue(result.preparation().stderr().contains("error:"));
        assertEquals(TestExecutionResult.Status.NOT_EXECUTED, result.tests().getFirst().status());
        assertNull(result.tests().getFirst().executionTimeNanos());
    }

    @Test
    @DisplayName("시간 초과 이후에도 다음 테스트를 실행합니다.")
    void timeLimitDoesNotPreventNextTest() throws Exception {
        // given
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C,
                "#include <stdio.h>\nint main(void) { if (getchar() == 'x') for (;;) {} puts(\"OK\"); }",
                mainHarness, List.of(new TestInput("loop", "x"), new TestInput("next", "y")));

        // when
        ExecutionResult result = service.execute(request, test -> {});

        // then
        assertEquals(TestExecutionResult.Status.TIME_LIMIT_EXCEEDED, result.tests().getFirst().status());
        assertEquals(TestExecutionResult.Status.COMPLETED, result.tests().getLast().status());
        assertEquals("OK\n", result.tests().getLast().stdout());
    }

    @Test
    @DisplayName("합산 출력 제한을 적용하고 다음 테스트를 실행합니다.")
    void outputLimitStopsBothStreamsAndAllowsNextTest() throws Exception {
        // given
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, """
                #include <stdio.h>
                int main(void) {
                    if (getchar() == 'x') {
                        for (;;) {
                            fputs("stdout\\n", stdout);
                            fputs("stderr\\n", stderr);
                        }
                    }
                    puts("OK");
                }
                """, mainHarness, List.of(new TestInput("flood", "x"), new TestInput("next", "y")));

        // when
        ExecutionResult result = service.execute(request, test -> {});

        TestExecutionResult limited = result.tests().getFirst();
        // then
        assertEquals(TestExecutionResult.Status.OUTPUT_LIMIT_EXCEEDED, limited.status());
        assertTrue((limited.stdout() + limited.stderr()).getBytes(StandardCharsets.UTF_8).length <= properties.outputLimitBytes());
        assertEquals(TestExecutionResult.Status.COMPLETED, result.tests().getLast().status());
        assertEquals("OK\n", result.tests().getLast().stdout());
    }

    @Test
    @DisplayName("종료 코드 124와 신호 종료 정보를 시간 초과와 구분합니다.")
    void exit124IsNotTimeLimitAndSignalsKeepExitInformation() throws Exception {
        // given
        // when
        ExecutionResult returned = service.execute(new ExecutionRequest(LanguageSpec.C, "int main(void) { return 124; }",
                mainHarness, List.of(new TestInput("exit", ""))), test -> {});
        // then
        assertEquals(TestExecutionResult.Status.COMPLETED, returned.tests().getFirst().status());
        assertEquals(124, returned.tests().getFirst().exitCode());

        ExecutionResult signalled = service.execute(new ExecutionRequest(LanguageSpec.C,
                "#include <signal.h>\nint main(void) { raise(SIGSEGV); }", mainHarness,
                List.of(new TestInput("signal", ""))), test -> {});
        assertEquals(TestExecutionResult.Status.COMPLETED, signalled.tests().getFirst().status());
        assertEquals(139, signalled.tests().getFirst().exitCode());
    }

    @Test
    @DisplayName("작업 중단 시 컨테이너와 임시 공간을 정리합니다.")
    void interruptedJobIsClosed() throws Exception {
        // given
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread thread = new Thread(() -> {
            try {
                service.execute(new ExecutionRequest(LanguageSpec.C, "int main(void) { for (;;) {} }", mainHarness,
                        List.of(new TestInput("loop", ""))), test -> {});
                failure.set(new AssertionError("작업이 중단되지 않았습니다."));
            } catch (InterruptedException exception) {
                if (!Thread.currentThread().isInterrupted()) {
                    failure.set(new AssertionError("중단 상태가 보존되지 않았습니다."));
                }
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });
        // when
        thread.start();
        Thread.sleep(500);
        thread.interrupt();
        thread.join(20000);
        // then
        assertFalse(thread.isAlive());
        assertNull(failure.get());
    }

    @AfterEach
    void leavesNoContainersOrWorkspaces() throws Exception {
        // given
        ProcessExecutor.Result containers = executor.execute(List.of("docker", "ps", "-a", "--filter", "name=" + containerPrefix,
                        "--format", "{{.Names}}"), null, Duration.ofSeconds(10), 1048576, () -> {});
        assertTrue(containers.succeeded(), containers.diagnostic());
        assertTrue(containers.stdout().isBlank(), containers.stdout());
        assertEquals(initialWorkspaces, workspaces());
    }

    private Set<Path> workspaces() {
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("algosteady-runner-"))
                    .collect(Collectors.toSet());
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
