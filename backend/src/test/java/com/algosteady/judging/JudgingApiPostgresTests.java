package com.algosteady.judging;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.dto.request.RunRequestDto;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.repository.SubmissionRepository;
import com.algosteady.training.service.SubmissionCommandService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL 기반 API 바인딩·저장·재조회 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class JudgingApiPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private ExecutionTestResultRepository executionTestResultRepository;

    @Autowired
    private JudgingCommandService judgingCommandService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionCommandService submissionCommandService;

    @Autowired
    private ExerciseQueryService exerciseQueryService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("JSON 실행 요청을 저장하고 DB에서 결과와 원본을 재조회합니다.")
    void register_success_andReloadResults() throws Exception {
        // given
        JsonNode detail = jsonMapper.readTree(mockMvc.perform(get("/api/exercises/stack-array?language=c")).andReturn().getResponse().getContentAsString());
        String exampleId = detail.path("publicExamples").get(0).path("id").asString();
        String code = "invalid code;";
        RunRequestDto request = RunRequestDto.builder()
                .exerciseId("stack-array")
                .contentVersion("1.0.0")
                .language("c")
                .code(code)
                .exampleId(exampleId)
                .build();

        // when
        ResultActions registration = mockMvc.perform(post("/api/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // then
        registration.andExpect(status().isAccepted());
        UUID jobId = UUID.fromString(jsonMapper.readTree(registration.andReturn().getResponse().getContentAsString()).path("jobId").asString());
        entityManager.flush();
        entityManager.clear();
        Assertions.assertEquals(code, executionJobRepository.findById(jobId).orElseThrow().getCode());

        // given
        TestExecutionResult testResult = new TestExecutionResult(exampleId, TestExecutionResult.Status.NOT_EXECUTED, "", "", null, null, "컴파일 실패");
        judgingCommandService.saveTest(jobId, testResult, detail.path("publicExamples").get(0).path("expected"));
        judgingCommandService.complete(jobId, new ExecutionResult(new PreparationResult(PreparationResult.Status.FAILED, "", "compiler error", 1, "컴파일 실패"), List.of(testResult)));
        entityManager.flush();
        entityManager.clear();

        // when
        ResultActions details = mockMvc.perform(get("/api/jobs/" + jobId));

        // then
        details.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.preparation.stderr").value("compiler error"))
                .andExpect(jsonPath("$.tests[0].passed").value(false));
        Assertions.assertEquals(1, executionTestResultRepository.findAllByJobId(jobId).size());
    }

    @Test
    @DisplayName("입력 형식, 지원 언어, 공개 예시와 없는 자원에 올바른 오류를 반환합니다.")
    void register_fail_withInvalidInput() throws Exception {
        // given
        String privateExampleRequest = """
                {"exerciseId":"stack-array","contentVersion":"1.0.0","language":"c","code":"x","exampleId":"private"}
                """;

        // when
        ResultActions missingFields = mockMvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content("{}"));
        ResultActions invalidJson = mockMvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content("{"));
        ResultActions privateExample = mockMvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(privateExampleRequest));
        ResultActions invalidLanguage = mockMvc.perform(get("/api/exercises/stack-array?language=java"));
        ResultActions missingExercise = mockMvc.perform(get("/api/exercises/missing?language=c"));
        ResultActions missingVersion = mockMvc.perform(get("/api/exercises/stack-array/reference?language=c&contentVersion=missing"));
        ResultActions missingJob = mockMvc.perform(get("/api/jobs/" + UUID.randomUUID()));
        ResultActions invalidJobId = mockMvc.perform(get("/api/jobs/not-a-uuid"));

        // then
        missingFields.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        invalidJson.andExpect(status().isBadRequest());
        privateExample.andExpect(status().isBadRequest());
        invalidLanguage.andExpect(status().isBadRequest());
        missingExercise.andExpect(status().isNotFound());
        missingVersion.andExpect(status().isNotFound());
        missingJob.andExpect(status().isNotFound());
        invalidJobId.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재시작 복구는 실행 중인 작업만 실패로 바꾸고 대기 작업을 보존합니다.")
    void recover_success_onlyRunningJobs() {
        // given
        ExecutionJob running = ExecutionJob.createGrading("stack-array", "1.0.0", "c", "code");
        running.markRunning();
        executionJobRepository.save(running);
        ExecutionJob queued = executionJobRepository.save(ExecutionJob.createGrading("stack-array", "1.0.0", "c", "code"));
        entityManager.flush();
        entityManager.clear();

        // when
        judgingCommandService.recoverInterrupted();
        entityManager.flush();
        entityManager.clear();

        // then
        Assertions.assertEquals(ExecutionJob.Status.FAILED, executionJobRepository.findById(running.getId()).orElseThrow().getStatus());
        Assertions.assertEquals(ExecutionJob.Status.QUEUED, executionJobRepository.findById(queued.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("매번 새 제출과 전체 채점 작업을 저장하고 기준별 결과를 재조회합니다.")
    void submit_success_withOriginalAndFullGrading() throws Exception {
        // given
        SubmissionRequestDto request = SubmissionRequestDto.builder()
                .exerciseId("stack-array")
                .contentVersion("1.0.0")
                .language("c")
                .code("original code")
                .referenceViewed(true)
                .build();
        String body = jsonMapper.writeValueAsString(request);

        // when
        ResultActions registration = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(body));

        // then
        registration.andExpect(status().isAccepted());
        JsonNode ids = jsonMapper.readTree(registration.andReturn().getResponse().getContentAsString());
        UUID submissionId = UUID.fromString(ids.path("submissionId").asString());
        UUID jobId = UUID.fromString(ids.path("jobId").asString());
        entityManager.flush();
        entityManager.clear();
        Submission original = submissionRepository.findById(submissionId).orElseThrow();
        Assertions.assertEquals("original code", original.getCode());
        Assertions.assertTrue(original.isReferenceViewed());
        Assertions.assertEquals(jobId, original.getJobId());
        Assertions.assertEquals(ExecutionJob.Kind.GRADING, executionJobRepository.findById(jobId).orElseThrow().getKind());

        // when
        ResultActions queuedJob = mockMvc.perform(get("/api/jobs/" + jobId));

        // then
        JsonNode queuedResponse = jsonMapper.readTree(queuedJob.andReturn().getResponse().getContentAsString());
        Assertions.assertEquals(8, queuedResponse.path("criteria").size());
        Assertions.assertTrue(queuedResponse.path("tests").isEmpty());
        Assertions.assertNull(executionJobRepository.findById(jobId).orElseThrow().getExampleId());

        // given
        JsonNode snapshot = exerciseQueryService.findSnapshot("stack-array", "1.0.0", "c");
        ArrayList<TestExecutionResult> actualResults = new ArrayList<>();
        for (JsonNode test : snapshot.path("sources").path("c").path("tests")) {
            JsonNode expected = test.path("expected");
            TestExecutionResult actual = new TestExecutionResult(test.path("id").asString(), TestExecutionResult.Status.COMPLETED,
                    expected.path("stdout").asString(), expected.path("stderrRequired").asBoolean() ? "오류" : "", expected.path("exitCode").asInt(), 1000L, null);
            judgingCommandService.saveTest(jobId, actual, expected);
            actualResults.add(actual);
        }
        judgingCommandService.complete(jobId, new ExecutionResult(new PreparationResult(PreparationResult.Status.SUCCEEDED, "", "", 0, null), actualResults));
        entityManager.flush();
        entityManager.clear();

        // when
        ResultActions completedJob = mockMvc.perform(get("/api/jobs/" + jobId));

        // then
        JsonNode completedResponse = jsonMapper.readTree(completedJob.andReturn().getResponse().getContentAsString());
        Assertions.assertEquals(22, completedResponse.path("tests").size());
        for (JsonNode criterion : completedResponse.path("criteria")) {
            Assertions.assertEquals("PASSED", criterion.path("status").asString());
        }

        // when
        ResultActions secondRegistration = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(body));

        // then
        JsonNode second = jsonMapper.readTree(secondRegistration.andReturn().getResponse().getContentAsString());
        Assertions.assertNotEquals(submissionId.toString(), second.path("submissionId").asString());
        Assertions.assertNotEquals(jobId.toString(), second.path("jobId").asString());
        Assertions.assertEquals("original code", submissionRepository.findById(submissionId).orElseThrow().getCode());
    }

    @Test
    @DisplayName("제출 저장 트랜잭션이 실패하면 채점 작업도 함께 롤백합니다.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void submit_fail_rollsBackSubmissionAndJobTogether() {
        // given
        AtomicReference<UUID> submissionId = new AtomicReference<>();
        AtomicReference<UUID> jobId = new AtomicReference<>();
        SubmissionRequestDto request = SubmissionRequestDto.builder()
                .exerciseId("stack-array")
                .contentVersion("1.0.0")
                .language("c")
                .code("original")
                .referenceViewed(false)
                .build();

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            SubmissionResponseDto response = submissionCommandService.register(request);
            submissionId.set(response.getSubmissionId());
            jobId.set(response.getJobId());
            entityManager.flush();
            throw new IllegalStateException("검증용 롤백");
        }));
        Assertions.assertFalse(submissionRepository.existsById(submissionId.get()));
        Assertions.assertFalse(executionJobRepository.existsById(jobId.get()));
    }

    @Test
    @DisplayName("잘못된 제출 입력은 채점 작업을 생성하지 않습니다.")
    void submit_fail_withInvalidInput() throws Exception {
        // given
        long jobsBefore = executionJobRepository.count();
        String missingReferenceViewedRequest = """
                {"exerciseId":"stack-array","contentVersion":"1.0.0","language":"c","code":"x"}
                """;
        String invalidLanguageRequest = """
                {"exerciseId":"stack-array","contentVersion":"1.0.0","language":"java","code":"x","referenceViewed":false}
                """;
        String missingVersionRequest = """
                {"exerciseId":"stack-array","contentVersion":"missing","language":"c","code":"x","referenceViewed":false}
                """;

        // when
        ResultActions missingFields = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("{}"));
        ResultActions missingReferenceViewed = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(missingReferenceViewedRequest));
        ResultActions invalidLanguage = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(invalidLanguageRequest));
        ResultActions missingVersion = mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content(missingVersionRequest));

        // then
        missingFields.andExpect(status().isBadRequest());
        missingReferenceViewed.andExpect(status().isBadRequest());
        invalidLanguage.andExpect(status().isBadRequest());
        missingVersion.andExpect(status().isNotFound());
        Assertions.assertEquals(jobsBefore, executionJobRepository.count());
    }
}
