package com.algosteady.judging.runner.service;

import com.algosteady.judging.runner.infrastructure.DockerContainerManager;
import com.algosteady.judging.runner.infrastructure.DockerExecutionSession;
import com.algosteady.judging.runner.language.LanguageSpec;
import com.algosteady.judging.runner.model.ExecutionRequest;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.runner.model.TestInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 테스트 실행 순서·결과 전달·오류 시 정리 검증 */
class CodeExecutionServiceTests {

    private final DockerContainerManager manager = mock(DockerContainerManager.class);
    private final DockerExecutionSession session = mock(DockerExecutionSession.class);
    private final CodeExecutionService service = new CodeExecutionService(manager);
    private final ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, "code", "harness",
            List.of(new TestInput("first", "1"), new TestInput("second", "2")));

    @Test
    @DisplayName("다음 테스트 전에 완료 결과를 전달합니다.")
    void reportsResultsBeforeStartingTheNextTest() throws Exception {
        // given
        when(manager.open(request)).thenReturn(session);
        when(session.prepare()).thenReturn(prepared());
        TestExecutionResult first = result("first", TestExecutionResult.Status.TIME_LIMIT_EXCEEDED);
        TestExecutionResult second = result("second", TestExecutionResult.Status.COMPLETED);
        List<TestExecutionResult> delivered = new ArrayList<>();
        when(session.execute(request.tests().getFirst())).thenReturn(first);
        when(session.execute(request.tests().getLast())).thenAnswer(call -> {
            assertEquals(List.of(first), delivered);
            return second;
        });

        // when
        ExecutionResult execution = service.execute(request, delivered::add);

        // then
        assertEquals(List.of(first, second), execution.tests());
        assertEquals(execution.tests(), delivered);
        verify(session).prepare();
        verify(session).close();
    }

    @Test
    @DisplayName("준비 실패 시 테스트를 실행하지 않습니다.")
    void preparationFailureDoesNotRunTests() throws Exception {
        // given
        when(manager.open(request)).thenReturn(session);
        when(session.prepare()).thenReturn(new PreparationResult(PreparationResult.Status.FAILED,
                "", "compiler error", 1, ""));
        List<TestExecutionResult> delivered = new ArrayList<>();

        // when
        ExecutionResult execution = service.execute(request, delivered::add);

        // then
        assertEquals(2, delivered.size());
        assertTrue(execution.tests().stream().allMatch(test -> test.status() == TestExecutionResult.Status.NOT_EXECUTED));
        verify(session, never()).execute(any());
        verify(session).close();
    }

    @Test
    @DisplayName("환경 장애 시 남은 테스트를 실행하지 않습니다.")
    void environmentErrorStopsRemainingTests() throws Exception {
        // given
        when(manager.open(request)).thenReturn(session);
        when(session.prepare()).thenReturn(prepared());
        when(session.execute(request.tests().getFirst())).thenReturn(result("first", TestExecutionResult.Status.ENVIRONMENT_ERROR));

        // when
        ExecutionResult execution = service.execute(request, result -> {});

        // then
        assertEquals(TestExecutionResult.Status.NOT_EXECUTED, execution.tests().getLast().status());
        verify(session, never()).execute(request.tests().getLast());
        verify(session).close();
    }

    @Test
    @DisplayName("정리 실패가 발생해도 원래 콜백 오류를 보존합니다.")
    void callbackFailurePreservesOriginalErrorWhenCleanupAlsoFails() throws Exception {
        // given
        when(manager.open(request)).thenReturn(session);
        when(session.prepare()).thenReturn(prepared());
        when(session.execute(any())).thenReturn(result("first", TestExecutionResult.Status.COMPLETED));
        IllegalStateException original = new IllegalStateException("result storage failed");
        IOException cleanup = new IOException("cleanup failed");
        doThrow(cleanup).when(session).close();

        // when
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.execute(request, result -> {
                    throw original;
                }));

        // then
        assertSame(original, thrown);
        assertArrayEquals(new Throwable[]{cleanup}, thrown.getSuppressed());
        verify(session).close();
    }

    @Test
    @DisplayName("중단된 세션을 정리하고 중단 상태를 보존합니다.")
    void interruptionClosesSessionAndPreservesInterruptFlag() throws Exception {
        // given
        when(manager.open(request)).thenReturn(session);
        when(session.prepare()).thenThrow(new InterruptedException("cancelled"));
        try {
            assertThrows(InterruptedException.class, () -> service.execute(request, result -> {}));
            assertTrue(Thread.currentThread().isInterrupted());
            verify(session).close();
        } finally {
            Thread.interrupted();
        }
    }

    private PreparationResult prepared() {
        return new PreparationResult(PreparationResult.Status.SUCCEEDED, "", "", 0, "");
    }

    private TestExecutionResult result(String id, TestExecutionResult.Status status) {
        return new TestExecutionResult(id, status, "", "", 0, 1L, "");
    }
}
