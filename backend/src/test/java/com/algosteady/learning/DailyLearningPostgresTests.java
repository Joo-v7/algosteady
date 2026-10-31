package com.algosteady.learning;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.domain.DailyLearningCompletion;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import com.algosteady.learning.repository.DailyLearningRepository;
import com.algosteady.learning.service.DailyLearningCommandService;
import com.algosteady.member.service.MemberCommandService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL 기반 일일 배정·채점 통과·중복·접수 날짜 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class DailyLearningPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DailyLearningRepository dailyLearningRepository;

    @Autowired
    private DailyLearningCompletionRepository dailyLearningCompletionRepository;

    @Autowired
    private DailyLearningCommandService dailyLearningCommandService;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private ExerciseQueryService exerciseQueryService;

    @Autowired
    private JudgingCommandService judgingCommandService;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private ExecutionTestResultRepository executionTestResultRepository;

    @Autowired
    private SubmissionCommandService submissionCommandService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDate learningDate = LocalDate.of(2026, 10, 24);
    private final Instant acceptedAt = Instant.parse("2026-10-24T20:59:59.999Z");
    private final PreparationResult successfulPreparation = new PreparationResult(PreparationResult.Status.SUCCEEDED, "", "", 0, null);
    private UUID memberId;
    private boolean createdLearningDate;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            memberId = memberCommandService.login("test", UUID.randomUUID().toString(), "daily@example.com", "완료 검증 회원").getId();
            createdLearningDate = !dailyLearningRepository.existsById(learningDate);
            dailyLearningRepository.assign(learningDate, "recursion-factorial");
        });
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @CsvSource({"2026-10-24T20:59:59.999999Z,2026-10-24", "2026-10-24T21:00:00Z,2026-10-25"})
    @DisplayName("채점이 나중에 끝나도 오전 6시 경계의 제출 접수 날짜로 완료를 저장합니다.")
    void complete_success_withAcceptedDate(String timestamp, String expectedDate) {
        // given
        LocalDate date = LocalDate.parse(expectedDate);
        dailyLearningRepository.assign(date, "recursion-factorial");
        ExecutionJob job = createGradingJob(memberId, "recursion-factorial", Instant.parse(timestamp));
        List<TestExecutionResult> results = savePassingTests(job);

        // when
        judgingCommandService.complete(job.getId(), new ExecutionResult(successfulPreparation, results));
        entityManager.flush();
        entityManager.clear();

        // then
        DailyLearningCompletion completion = dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, date).orElseThrow();
        ExecutionJob storedJob = executionJobRepository.findById(job.getId()).orElseThrow();
        Assertions.assertEquals(job.getId(), completion.getJobId());
        Assertions.assertEquals(storedJob.getCompletedAt(), completion.getCompletedAt());
        Assertions.assertTrue(completion.getCompletedAt().isAfter(Instant.parse(timestamp)));
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, date.plusDays(1)).isEmpty());
    }

    @Test
    @DisplayName("재통과·중복 완료·이후 실패에도 최초 기록을 유지하고 다른 회원은 따로 기록합니다.")
    void complete_success_oncePerMemberAndDay() {
        // given
        UUID otherMemberId = memberCommandService.login("test", UUID.randomUUID().toString(), "other@example.com", "다른 회원").getId();
        long countBefore = dailyLearningCompletionRepository.count();
        ExecutionJob first = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        ExecutionJob repeated = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        ExecutionJob other = createGradingJob(otherMemberId, "recursion-factorial", acceptedAt);

        // when
        judgingCommandService.complete(first.getId(), new ExecutionResult(successfulPreparation, savePassingTests(first)));
        judgingCommandService.complete(first.getId(), new ExecutionResult(successfulPreparation, List.of()));
        judgingCommandService.complete(repeated.getId(), new ExecutionResult(successfulPreparation, savePassingTests(repeated)));
        judgingCommandService.complete(other.getId(), new ExecutionResult(successfulPreparation, savePassingTests(other)));
        ExecutionJob failed = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        judgingCommandService.complete(failed.getId(), new ExecutionResult(new PreparationResult(PreparationResult.Status.FAILED, "", "compile error", 1, null), List.of()));
        entityManager.flush();
        entityManager.clear();

        // then
        Assertions.assertEquals(countBefore + 2, dailyLearningCompletionRepository.count());
        Assertions.assertEquals(first.getId(), dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).orElseThrow().getJobId());
        Assertions.assertEquals(other.getId(), dailyLearningCompletionRepository.findByMemberIdAndLearningDate(otherMemberId, learningDate).orElseThrow().getJobId());
    }

    @ParameterizedTest
    @EnumSource(value = PreparationResult.Status.class, names = {"FAILED", "TIME_LIMIT_EXCEEDED", "OUTPUT_LIMIT_EXCEEDED", "ENVIRONMENT_ERROR"})
    @DisplayName("컴파일 실패·준비 제한 초과·실행 환경 장애는 일일 완료로 인정하지 않습니다.")
    void complete_fail_withPreparationFailure(PreparationResult.Status preparationStatus) {
        // given
        ExecutionJob job = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        PreparationResult preparation = new PreparationResult(preparationStatus, "", "failure", null, "failure");

        // when
        judgingCommandService.complete(job.getId(), new ExecutionResult(preparation, List.of()));

        // then
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TestExecutionResult.Status.class)
    @DisplayName("오답·시간 초과·출력 초과·장애·미실행 테스트가 하나라도 있으면 완료하지 않습니다.")
    void complete_fail_withFailedRequiredTest(TestExecutionResult.Status failureStatus) {
        // given
        ExecutionJob job = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        List<TestExecutionResult> results = new ArrayList<>();
        for (JsonNode test : testsFor(job)) {
            TestExecutionResult actual = passingResult(test);
            if (results.isEmpty()) {
                actual = new TestExecutionResult(actual.testId(), failureStatus, actual.stdout() + "wrong", actual.stderr(), actual.exitCode(), 1L, "failure");
            }
            judgingCommandService.saveTest(job.getId(), actual, test.path("expected"));
            results.add(actual);
        }

        // when
        judgingCommandService.complete(job.getId(), new ExecutionResult(successfulPreparation, results));

        // then
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isEmpty());
    }

    @Test
    @DisplayName("필수 테스트가 누락되면 무관한 통과 결과로 개수를 채워도 완료하지 않습니다.")
    void complete_fail_withMissingRequiredTest() {
        // given
        ExecutionJob job = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        JsonNode tests = testsFor(job);
        List<TestExecutionResult> results = new ArrayList<>();
        for (int index = 0; index < tests.size() - 1; index++) {
            JsonNode test = tests.get(index);
            TestExecutionResult actual = passingResult(test);
            judgingCommandService.saveTest(job.getId(), actual, test.path("expected"));
            results.add(actual);
        }
        executionTestResultRepository.save(ExecutionTestResult.create(job.getId(), "unregistered-test", "{}", true));

        // when
        judgingCommandService.complete(job.getId(), new ExecutionResult(successfulPreparation, results));

        // then
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isEmpty());
    }

    @Test
    @DisplayName("다른 과제의 통과와 공개 예제 실행은 오늘의 학습 완료로 인정하지 않습니다.")
    void complete_fail_withOtherExerciseOrExampleRun() {
        // given
        ExecutionJob other = createGradingJob(memberId, "stack-array", acceptedAt);
        String version = exerciseQueryService.findDetails("recursion-factorial", "c").getVersion();
        ExecutionJob run = executionJobRepository.save(ExecutionJob.create("recursion-factorial", version, "c", "code", "example"));

        // when
        judgingCommandService.complete(other.getId(), new ExecutionResult(successfulPreparation, savePassingTests(other)));
        judgingCommandService.complete(run.getId(), new ExecutionResult(successfulPreparation, List.of()));

        // then
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isEmpty());
    }

    @Test
    @DisplayName("배정된 과제는 같은 날짜에 새 배정을 요청해도 바뀌지 않습니다.")
    void assign_success_preservesFirstAssignment() {
        // when
        dailyLearningRepository.assign(learningDate, "stack-array");
        DailyLearning dailyLearning = dailyLearningCommandService.assign(learningDate);

        // then
        Assertions.assertEquals("recursion-factorial", dailyLearning.getExerciseId());
    }

    @Test
    @DisplayName("홈 진입 없이 제출해도 접수 날짜의 배정을 저장하고 제출·작업 시각을 일치시킵니다.")
    void register_success_usesSameAcceptedAt() {
        // given
        String version = exerciseQueryService.findDetails("recursion-factorial", "c").getVersion();
        SubmissionRequestDto request = SubmissionRequestDto.builder()
                .exerciseId("recursion-factorial")
                .contentVersion(version)
                .language("c")
                .code("code")
                .referenceViewed(false)
                .build();

        // when
        SubmissionResponseDto response = submissionCommandService.register(memberId, request);
        entityManager.flush();
        entityManager.clear();

        // then
        Instant submittedAt = submissionRepository.findById(response.getSubmissionId()).orElseThrow().getSubmittedAt();
        Assertions.assertEquals(submittedAt, executionJobRepository.findById(response.getJobId()).orElseThrow().getQueuedAt());
        Assertions.assertTrue(dailyLearningRepository.existsById(DailyLearning.learningDateAt(submittedAt)));
    }

    @Test
    @DisplayName("오늘의 과제는 공통으로 반환하고 완료 여부는 로그인한 본인에게만 반환합니다.")
    void findToday_success_withMemberCompletion() throws Exception {
        // given
        LocalDate today = DailyLearning.learningDateAt(Instant.now());
        DailyLearning dailyLearning = dailyLearningCommandService.assign(today);
        ExecutionJob job = createGradingJob(memberId, dailyLearning.getExerciseId(), Instant.now());
        judgingCommandService.complete(job.getId(), new ExecutionResult(successfulPreparation, savePassingTests(job)));
        DefaultOAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OIDC_USER")), Map.of("member_id", memberId.toString()), "member_id");

        // when / then
        mockMvc.perform(get("/api/learning/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(dailyLearning.getExerciseId()))
                .andExpect(jsonPath("$.completed").value(false));
        mockMvc.perform(get("/api/learning/today").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningDate").value(today.toString()))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("채점 완료 트랜잭션을 롤백하면 일일 완료 기록도 함께 롤백합니다.")
    void complete_fail_rollsBackTogether() {
        // given
        ExecutionJob job = createGradingJob(memberId, "recursion-factorial", acceptedAt);
        List<TestExecutionResult> results = savePassingTests(job);
        try {
            // when
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                judgingCommandService.complete(job.getId(), new ExecutionResult(successfulPreparation, results));
                status.setRollbackOnly();
            });

            // then
            Assertions.assertEquals(ExecutionJob.Status.QUEUED, executionJobRepository.findById(job.getId()).orElseThrow().getStatus());
            Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isEmpty());
        } finally {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.update("DELETE FROM daily_learning_completion WHERE job_id = ?", job.getId());
                jdbcTemplate.update("DELETE FROM execution_test_result WHERE job_id = ?", job.getId());
                executionJobRepository.deleteById(job.getId());
                entityManager.flush();
                jdbcTemplate.update("DELETE FROM member_account WHERE id = ?", memberId);
                if (createdLearningDate) {
                    dailyLearningRepository.deleteById(learningDate);
                }
            });
        }
    }

    private ExecutionJob createGradingJob(UUID ownerId, String exerciseId, Instant receivedAt) {
        String version = exerciseQueryService.findDetails(exerciseId, "c").getVersion();
        UUID jobId = judgingCommandService.registerGrading(ownerId, exerciseId, version, "c", "code", receivedAt);
        return executionJobRepository.findById(jobId).orElseThrow();
    }

    private JsonNode testsFor(ExecutionJob job) {
        return exerciseQueryService.findSnapshot(job.getExerciseId(), job.getContentVersion(), "c").path("sources").path("c").path("tests");
    }

    private List<TestExecutionResult> savePassingTests(ExecutionJob job) {
        List<TestExecutionResult> results = new ArrayList<>();
        for (JsonNode test : testsFor(job)) {
            TestExecutionResult actual = passingResult(test);
            judgingCommandService.saveTest(job.getId(), actual, test.path("expected"));
            results.add(actual);
        }
        return results;
    }

    private TestExecutionResult passingResult(JsonNode test) {
        JsonNode expected = test.path("expected");
        return new TestExecutionResult(test.path("id").asString(), TestExecutionResult.Status.COMPLETED, expected.path("stdout").asString(), expected.path("stderrRequired").asBoolean() ? "expected error" : "", expected.path("exitCode").asInt(), 1L, null);
    }
}
