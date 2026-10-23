package com.algosteady.judging;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.repository.ExecutionTestSummary;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.JudgingQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 채점 기준별 상태 및 원본 테스트 연결 검증 */
@ExtendWith(MockitoExtension.class)
class JudgingQueryServiceTests {

    @InjectMocks
    private JudgingQueryService judgingQueryService;

    @Mock
    private ExecutionJobRepository executionJobRepository;

    @Mock
    private ExecutionTestResultRepository executionTestResultRepository;

    @Mock
    private ExerciseQueryService exerciseQueryService;

    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().build();

    private ExecutionJob executionJob;
    private final List<ExecutionTestResult> storedResults = new ArrayList<>();
    private JsonNode testContents;
    private JsonNode exerciseMetadata;

    @BeforeEach
    void setUp() throws Exception {
        ObjectNode snapshot = jsonMapper.createObjectNode();
        exerciseMetadata = jsonMapper.readTree(new ClassPathResource("content/stack-array/exercise.json").getContentAsString(StandardCharsets.UTF_8));
        executionJob = ExecutionJob.createGrading(exerciseMetadata.path("id").asString(), exerciseMetadata.path("version").asString(), "c", "code");
        snapshot.set("exercise", exerciseMetadata);
        testContents = jsonMapper.readTree(new ClassPathResource("content/stack-array/c/tests.json").getContentAsString(StandardCharsets.UTF_8));
        snapshot.putObject("sources").putObject("c").set("tests", testContents);
        Mockito.when(exerciseQueryService.findSnapshot(executionJob.getExerciseId(), executionJob.getContentVersion(), "c")).thenReturn(snapshot);
        Mockito.when(executionJobRepository.findById(executionJob.getId())).thenReturn(Optional.of(executionJob));
        Mockito.when(executionTestResultRepository.findAllByJobId(executionJob.getId())).thenReturn(storedResults);
    }

    @Test
    @DisplayName("전체 테스트가 통과해야 기준을 통과시키고 각 입력과 기대값을 원본에 연결합니다.")
    void findById_allReferenceResultsPass() {
        // given
        for (JsonNode test : testContents) {
            storedResults.add(createStoredResult(test, TestExecutionResult.Status.COMPLETED, true));
        }
        executionJob.complete("{}", false);

        // when
        JobResponseDto response = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals(testContents.size(), response.getTests().size());
        Assertions.assertEquals(exerciseMetadata.path("criteria").size(), response.getCriteria().size());
        Assertions.assertTrue(response.getCriteria().stream()
                .allMatch(criterion -> criterion.getStatus().equals("PASSED")));
        for (int index = 0; index < testContents.size(); index++) {
            Assertions.assertEquals(testContents.get(index).path("input").asString(), response.getTests().get(index).getInput());
            Assertions.assertEquals(testContents.get(index).path("expected"), response.getTests().get(index).getExpected());
        }
    }

    @Test
    @DisplayName("일부 테스트만 통과했을 때 전체 기준을 통과 처리하지 않습니다.")
    void findById_showsProgress() {
        // given
        executionJob.markRunning();

        // when
        JobResponseDto beforeTests = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals("RUNNING", beforeTests.getCriteria().getFirst().getStatus());

        // given
        storedResults.add(createStoredResult(testContents.get(0), TestExecutionResult.Status.COMPLETED, true));

        // when
        JobResponseDto afterFirstTest = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals("RUNNING", afterFirstTest.getCriteria().getFirst().getStatus());

        // given
        storedResults.add(createStoredResult(testContents.get(1), TestExecutionResult.Status.COMPLETED, true));

        // when
        JobResponseDto afterCriterionCompleted = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals("PASSED", afterCriterionCompleted.getCriteria().get(0).getStatus());
        Assertions.assertEquals("RUNNING", afterCriterionCompleted.getCriteria().get(1).getStatus());
        Assertions.assertEquals("PENDING", afterCriterionCompleted.getCriteria().get(2).getStatus());
    }

    @Test
    @DisplayName("시간 초과로 실패한 기준과 중단 때문에 미실행된 기준을 구분합니다.")
    void findById_preservesFailureAfterInterruption() {
        // given
        storedResults.add(createStoredResult(testContents.get(0), TestExecutionResult.Status.TIME_LIMIT_EXCEEDED, false));
        executionJob.markFailed("서버 중단");

        // when
        JobResponseDto response = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals("FAILED", response.getCriteria().getFirst().getStatus());
        Assertions.assertTrue(response.getCriteria().stream()
                .skip(1)
                .allMatch(criterion -> criterion.getStatus().equals("UNAVAILABLE")));
    }

    @Test
    @DisplayName("컴파일 실패로 미실행된 테스트는 기능 실패 대신 검사 불가입니다.")
    void findById_compilationFailureIsUnavailable() {
        // given
        for (JsonNode test : testContents) {
            storedResults.add(createStoredResult(test, TestExecutionResult.Status.NOT_EXECUTED, false));
        }
        executionJob.complete("{}", false);

        // when
        JobResponseDto response = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertTrue(response.getCriteria().stream()
                .allMatch(criterion -> criterion.getStatus().equals("UNAVAILABLE")));
    }

    @Test
    @DisplayName("환경 장애 전에 통과한 기준은 보존하고 나머지는 검사 불가입니다.")
    void findById_preservesCompletedCriteriaOnEnvironmentFailure() {
        // given
        storedResults.add(createStoredResult(testContents.get(0), TestExecutionResult.Status.COMPLETED, true));
        storedResults.add(createStoredResult(testContents.get(1), TestExecutionResult.Status.COMPLETED, true));
        storedResults.add(createStoredResult(testContents.get(2), TestExecutionResult.Status.ENVIRONMENT_ERROR, false));
        executionJob.complete("{}", true);

        // when
        JobResponseDto response = judgingQueryService.findById(executionJob.getId());

        // then
        Assertions.assertEquals("PASSED", response.getCriteria().getFirst().getStatus());
        Assertions.assertTrue(response.getCriteria().stream()
                .skip(1)
                .allMatch(criterion -> criterion.getStatus().equals("UNAVAILABLE")));
    }

    @Test
    @DisplayName("중단된 작업도 상세와 기록 요약에서 같은 통과 기준 수를 반환합니다.")
    void findSummariesByIds_success_matchesDetailsAfterInterruption() {
        // given
        storedResults.add(createStoredResult(testContents.get(0), TestExecutionResult.Status.COMPLETED, true));
        storedResults.add(createStoredResult(testContents.get(1), TestExecutionResult.Status.COMPLETED, true));
        storedResults.add(createStoredResult(testContents.get(2), TestExecutionResult.Status.COMPLETED, true));
        storedResults.add(createStoredResult(testContents.get(3), TestExecutionResult.Status.TIME_LIMIT_EXCEEDED, false));
        executionJob.markFailed("서버 중단");
        List<ExecutionTestSummary> testSummaries = new ArrayList<>();
        for (ExecutionTestResult result : storedResults) {
            ExecutionTestSummary summary = Mockito.mock(ExecutionTestSummary.class);
            Mockito.when(summary.getPassed()).thenReturn(result.isPassed());
            if (result.isPassed()) {
                Mockito.when(summary.getJobId()).thenReturn(executionJob.getId());
                Mockito.when(summary.getTestId()).thenReturn(result.getTestId());
            }
            testSummaries.add(summary);
        }
        Mockito.when(executionJobRepository.findAllById(List.of(executionJob.getId()))).thenReturn(List.of(executionJob));
        Mockito.when(executionTestResultRepository.findSummariesByJobIds(List.of(executionJob.getId()))).thenReturn(testSummaries);

        // when
        JobResponseDto details = judgingQueryService.findById(executionJob.getId());
        JobSummaryResponseDto summary = judgingQueryService.findSummariesByIds(List.of(executionJob.getId())).get(executionJob.getId());

        // then
        Assertions.assertEquals(1, summary.getPassedCriteria());
        Assertions.assertEquals(details.getCriteria().stream().filter(criterion -> criterion.getStatus().equals("PASSED")).count(), summary.getPassedCriteria());
        Assertions.assertEquals(details.getCriteria().size(), summary.getTotalCriteria());
        Assertions.assertEquals(details.getStatus(), summary.getStatus());
    }

    private ExecutionTestResult createStoredResult(JsonNode test, TestExecutionResult.Status status, boolean passed) {
        TestExecutionResult actual = new TestExecutionResult(test.path("id").asString(), status, "", "", 0, 1000L, null);
        return ExecutionTestResult.create(executionJob.getId(), actual.testId(), jsonMapper.writeValueAsString(actual), passed);
    }
}
