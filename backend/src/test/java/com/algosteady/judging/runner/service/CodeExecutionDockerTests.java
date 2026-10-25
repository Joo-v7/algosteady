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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Docker 기반 C 실행·제한·자원 정리 검증 */
@EnabledIfSystemProperty(named = "algosteady.docker.tests", matches = "true")
class CodeExecutionDockerTests {

    private final DockerExecutionFixture fixture = new DockerExecutionFixture();
    private final String mainHarness = "#include \"submission.c\"\n";

    @Test
    @DisplayName("컴파일 실패 시 테스트를 실행하지 않습니다.")
    void compilationFailureDoesNotExecuteTests() throws Exception {
        // given
        // when
        ExecutionResult result = fixture.codeExecutionService.execute(new ExecutionRequest(LanguageSpec.C, "invalid C code;", mainHarness,
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
        ExecutionResult result = fixture.codeExecutionService.execute(request, test -> {});

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
        ExecutionResult result = fixture.codeExecutionService.execute(request, test -> {});

        TestExecutionResult limited = result.tests().getFirst();
        // then
        assertEquals(TestExecutionResult.Status.OUTPUT_LIMIT_EXCEEDED, limited.status());
        assertTrue((limited.stdout() + limited.stderr()).getBytes(StandardCharsets.UTF_8).length <= fixture.runnerProperties.outputLimitBytes());
        assertEquals(TestExecutionResult.Status.COMPLETED, result.tests().getLast().status());
        assertEquals("OK\n", result.tests().getLast().stdout());
    }

    @Test
    @DisplayName("종료 코드 124와 신호 종료 정보를 시간 초과와 구분합니다.")
    void exit124IsNotTimeLimitAndSignalsKeepExitInformation() throws Exception {
        // given
        // when
        ExecutionResult returned = fixture.codeExecutionService.execute(new ExecutionRequest(LanguageSpec.C, "int main(void) { return 124; }",
                mainHarness, List.of(new TestInput("exit", ""))), test -> {});
        // then
        assertEquals(TestExecutionResult.Status.COMPLETED, returned.tests().getFirst().status());
        assertEquals(124, returned.tests().getFirst().exitCode());

        ExecutionResult signalled = fixture.codeExecutionService.execute(new ExecutionRequest(LanguageSpec.C,
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
                fixture.codeExecutionService.execute(new ExecutionRequest(LanguageSpec.C, "int main(void) { for (;;) {} }", mainHarness,
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
        fixture.assertCleanedUp();
    }
}
