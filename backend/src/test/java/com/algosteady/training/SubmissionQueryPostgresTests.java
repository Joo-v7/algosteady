package com.algosteady.training;

import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.repository.SubmissionRepository;
import com.algosteady.training.service.SubmissionCommandService;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL 기반 제출 조회·페이지·요약·원본 보존 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class SubmissionQueryPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ExerciseContentRepository exerciseContentRepository;

    @Autowired
    private ExerciseQueryService exerciseQueryService;

    @Autowired
    private SubmissionCommandService submissionCommandService;

    @Autowired
    private JudgingCommandService judgingCommandService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;
    private String exerciseId;
    private String contentVersion;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        exerciseId = "history-" + UUID.randomUUID();
        ExerciseContent original = exerciseContentRepository.findFirstByExerciseIdOrderByIdDesc("stack-array").orElseThrow();
        contentVersion = original.getContentVersion();
        JsonNode snapshot = jsonMapper.readTree(original.getSnapshot());
        ((ObjectNode) snapshot.path("exercise")).put("id", exerciseId);
        exerciseContentRepository.saveAndFlush(ExerciseContent.create(exerciseId, contentVersion, jsonMapper.writeValueAsString(snapshot)));
    }

    @Test
    @DisplayName("최신순 페이지에서 출력 없이 통과 기준을 집계하고 상세에서는 원본과 결과를 조회합니다.")
    void findAll_success_withStoredResults() throws Exception {
        // given
        SubmissionResponseDto older = registerSubmission("older original", false);
        SubmissionResponseDto latest = registerSubmission("latest original", true);
        JsonNode snapshot = exerciseQueryService.findSnapshot(exerciseId, contentVersion, "c");
        ArrayList<TestExecutionResult> testResults = new ArrayList<>();
        for (JsonNode test : snapshot.path("sources").path("c").path("tests")) {
            JsonNode expected = test.path("expected");
            TestExecutionResult result = new TestExecutionResult(test.path("id").asString(), TestExecutionResult.Status.COMPLETED,
                    expected.path("stdout").asString(), expected.path("stderrRequired").asBoolean() ? "오류" : "", expected.path("exitCode").asInt(), 1000L, null);
            judgingCommandService.saveTest(latest.getJobId(), result, expected);
            testResults.add(result);
        }
        judgingCommandService.complete(latest.getJobId(), new ExecutionResult(new PreparationResult(PreparationResult.Status.SUCCEEDED, "", "", 0, null), testResults));
        entityManager.flush();
        entityManager.clear();

        // when
        ResultActions firstPage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("size", "1"));
        ResultActions secondPage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("size", "1").param("page", "1"));
        ResultActions details = mockMvc.perform(get("/api/submissions/" + latest.getSubmissionId()));

        // then
        firstPage.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].submissionId").value(latest.getSubmissionId().toString()))
                .andExpect(jsonPath("$.content[0].job.status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].job.passedCriteria").value(snapshot.path("exercise").path("criteria").size()))
                .andExpect(jsonPath("$.content[0].code").doesNotExist())
                .andExpect(jsonPath("$.content[0].job.tests").doesNotExist());
        secondPage.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].submissionId").value(older.getSubmissionId().toString()))
                .andExpect(jsonPath("$.content[0].job.status").value("QUEUED"))
                .andExpect(jsonPath("$.content[0].job.passedCriteria").value(0));
        details.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("latest original"))
                .andExpect(jsonPath("$.referenceViewed").value(true))
                .andExpect(jsonPath("$.contentVersion").value(contentVersion))
                .andExpect(jsonPath("$.job.jobId").value(latest.getJobId().toString()))
                .andExpect(jsonPath("$.job.tests.length()").value(snapshot.path("sources").path("c").path("tests").size()));
        Assertions.assertThat(submissionRepository.findById(older.getSubmissionId()).orElseThrow().getCode()).isEqualTo("older original");
        Assertions.assertThat(submissionRepository.findById(latest.getSubmissionId()).orElseThrow().getCode()).isEqualTo("latest original");
    }

    @Test
    @DisplayName("빈 목록과 범위를 벗어난 페이지는 빈 내용으로 반환합니다.")
    void findAll_success_whenEmptyOrOutOfRange() throws Exception {
        // when
        ResultActions emptyPage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c"));

        // then
        emptyPage.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // given
        registerSubmission("original", false);

        // when
        ResultActions outOfRangePage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("page", "99"));

        // then
        outOfRangePage.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("잘못된 조회 조건과 없는 제출을 한국어 오류로 반환하며 데이터를 변경하지 않습니다.")
    void findAll_fail_withInvalidQueries() throws Exception {
        // given
        long jobsBefore = executionJobRepository.count();

        // when
        ResultActions invalidLanguage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "java"));
        ResultActions negativePage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("page", "-1"));
        ResultActions zeroSize = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("size", "0"));
        ResultActions invalidPage = mockMvc.perform(get("/api/submissions").param("exerciseId", exerciseId).param("language", "c").param("page", "invalid"));
        ResultActions invalidSubmissionId = mockMvc.perform(get("/api/submissions/invalid"));
        ResultActions missingSubmission = mockMvc.perform(get("/api/submissions/" + UUID.randomUUID()));

        // then
        invalidLanguage.andExpect(status().isBadRequest());
        negativePage.andExpect(status().isBadRequest());
        zeroSize.andExpect(status().isBadRequest());
        invalidPage.andExpect(status().isBadRequest());
        invalidSubmissionId.andExpect(status().isBadRequest());
        missingSubmission.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
        Assertions.assertThat(executionJobRepository.count()).isEqualTo(jobsBefore);
    }

    private SubmissionResponseDto registerSubmission(String code, boolean referenceViewed) {
        return submissionCommandService.register(SubmissionRequestDto.builder()
                .exerciseId(exerciseId)
                .contentVersion(contentVersion)
                .language("c")
                .code(code)
                .referenceViewed(referenceViewed)
                .build());
    }
}
