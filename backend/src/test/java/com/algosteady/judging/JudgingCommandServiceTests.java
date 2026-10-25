package com.algosteady.judging;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.JudgingCommandService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 기대값 비교 및 프로그램 오류·시스템 장애 구분 검증 */
@ExtendWith(MockitoExtension.class)
class JudgingCommandServiceTests {

    @InjectMocks
    private JudgingCommandService judgingCommandService;

    @Mock
    private ExecutionJobRepository executionJobRepository;

    @Mock
    private ExecutionTestResultRepository executionTestResultRepository;

    @Mock
    private ExerciseQueryService exerciseQueryService;

    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("줄바꿈을 정규화하되 출력 공백과 종료 코드는 구분합니다.")
    void saveTest_normalizesOnlyLineEndings() {
        // given
        JsonNode expected = jsonMapper.readTree("{\"stdout\":\"OK\\n\",\"exitCode\":1,\"stderrRequired\":true}");
        UUID jobId = UUID.randomUUID();

        // when
        judgingCommandService.saveTest(jobId, new TestExecutionResult("example", TestExecutionResult.Status.COMPLETED, "OK\r\n", "error", 1, 10L, null), expected);
        judgingCommandService.saveTest(jobId, new TestExecutionResult("example", TestExecutionResult.Status.COMPLETED, "OK", "error", 1, 10L, null), expected);
        judgingCommandService.saveTest(jobId, new TestExecutionResult("example", TestExecutionResult.Status.COMPLETED, "OK \n", "error", 1, 10L, null), expected);
        judgingCommandService.saveTest(jobId, new TestExecutionResult("example", TestExecutionResult.Status.COMPLETED, "OK", "error", 0, 10L, null), expected);
        judgingCommandService.saveTest(jobId, new TestExecutionResult("example", TestExecutionResult.Status.COMPLETED, "OK", "", 1, 10L, null), expected);

        // then
        ArgumentCaptor<ExecutionTestResult> captured = ArgumentCaptor.forClass(ExecutionTestResult.class);
        Mockito.verify(executionTestResultRepository, Mockito.times(5)).save(captured.capture());
        Assertions.assertEquals(List.of(true, true, false, false, false), captured.getAllValues().stream()
                .map(ExecutionTestResult::isPassed)
                .toList());
    }

    @Test
    @DisplayName("컴파일 실패는 완료 결과로 저장하고 실행 환경 장애는 작업 실패로 저장합니다.")
    void complete_distinguishesProgramAndEnvironmentFailure() {
        // given
        ExecutionJob executionJob = ExecutionJob.create("stack-array", "1.0.0", "c", "code", "example");
        Mockito.when(executionJobRepository.findById(executionJob.getId())).thenReturn(Optional.of(executionJob));
        ExecutionResult compilationFailure = new ExecutionResult(new PreparationResult(PreparationResult.Status.FAILED, "", "error", 1, null), List.of());
        ExecutionResult environmentFailure = new ExecutionResult(new PreparationResult(PreparationResult.Status.ENVIRONMENT_ERROR, "", "docker error", null, null), List.of());

        // when
        judgingCommandService.complete(executionJob.getId(), compilationFailure);

        // then
        Assertions.assertEquals(ExecutionJob.Status.COMPLETED, executionJob.getStatus());

        // when
        judgingCommandService.complete(executionJob.getId(), environmentFailure);

        // then
        Assertions.assertEquals(ExecutionJob.Status.FAILED, executionJob.getStatus());
        Assertions.assertNotNull(executionJob.getCompletedAt());
    }
}
