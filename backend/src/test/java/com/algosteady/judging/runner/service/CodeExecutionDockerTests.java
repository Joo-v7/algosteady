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
    private final Path content = Path.of("../content/stack-array/c");
    private final String mainHarness = "#include \"submission.c\"\n";
    private final Set<Path> initialWorkspaces = workspaces();
    private final String containerPrefix = "algosteady-runner-";

    @Test
    @DisplayName("스택 22개 테스트를 통과하고 결과를 순서대로 전달합니다.")
    void passesAllReferenceTestsAndDeliversResultsInOrder() throws Exception {
        // given
        JsonNode tests = JsonMapper.builder()
                .build()
                .readTree(Files.readString(content.resolve("tests.json")));
        List<TestInput> inputs = new ArrayList<>();
        tests.forEach(test -> inputs.add(new TestInput(test.get("id").asString(), test.get("input").asString())));
        List<TestExecutionResult> delivered = new ArrayList<>();
        ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, Files.readString(content.resolve("reference.c")),
                Files.readString(content.resolve("harness.c")), inputs);

        // when
        ExecutionResult result = service.execute(request, delivered::add);

        // then
        assertEquals(PreparationResult.Status.SUCCEEDED, result.preparation().status());
        assertEquals(22, result.tests().size());
        assertEquals(result.tests(), delivered);
        for (int index = 0; index < tests.size(); index++) {
            JsonNode expected = tests.get(index).get("expected");
            TestExecutionResult actual = result.tests().get(index);
            assertEquals(inputs.get(index).id(), actual.testId());
            assertEquals(TestExecutionResult.Status.COMPLETED, actual.status(), actual.toString());
            assertEquals(expected.get("stdout").asString(), actual.stdout());
            assertEquals(expected.get("exitCode").asInt(), actual.exitCode());
            assertEquals(expected.get("stderrRequired").asBoolean(), !actual.stderr().isBlank());
            assertTrue(actual.executionTimeNanos() >= 0);
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
