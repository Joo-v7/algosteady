package com.algosteady.training;

import com.algosteady.exercise.dto.response.ExerciseDetailsResponseDto;
import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.member.service.MemberCommandService;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.repository.SubmissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 비회원 실행 허용 및 제출·채점 결과의 회원별 접근 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class SubmissionAccessPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private ExerciseQueryService exerciseQueryService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;
    private UUID memberId;
    private UUID otherMemberId;
    private ExerciseDetailsResponseDto exercise;
    private String submissionBody;

    @BeforeEach
    void setUp() {
        memberId = memberCommandService.login("test", UUID.randomUUID().toString(), "owner@example.com", "제출 회원").getId();
        otherMemberId = memberCommandService.login("test", UUID.randomUUID().toString(), "other@example.com", "다른 회원").getId();
        exercise = exerciseQueryService.findDetails("stack-array", "c");
        submissionBody = jsonMapper.writeValueAsString(SubmissionRequestDto.builder()
                .exerciseId(exercise.getId())
                .contentVersion(exercise.getVersion())
                .language("c")
                .code("private submission code")
                .referenceViewed(false)
                .build());
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("비회원도 공개 예제를 실행하고 해당 실행 결과를 조회합니다.")
    void run_success_withoutLogin() throws Exception {
        // given
        String body = jsonMapper.writeValueAsString(Map.of(
                "exerciseId", exercise.getId(),
                "contentVersion", exercise.getVersion(),
                "language", "c",
                "code", "public example code",
                "exampleId", exercise.getPublicExamples().get(0).path("id").asString()));

        // when
        JsonNode response = jsonMapper.readTree(mockMvc.perform(post("/api/runs").with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());

        // then
        mockMvc.perform(get("/api/jobs/" + response.path("jobId").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("RUN"))
                .andExpect(jsonPath("$.code").value("public example code"));
    }

    @Test
    @DisplayName("비회원의 제출·기록 요청은 401이며 작업과 제출을 생성하지 않습니다.")
    void submit_fail_withoutLogin() throws Exception {
        // given
        long jobsBefore = executionJobRepository.count();
        long submissionsBefore = submissionRepository.count();

        // when / then
        mockMvc.perform(post("/api/submissions").with(csrf()).contentType("application/json").content(submissionBody))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/submissions").param("exerciseId", exercise.getId()).param("language", "c"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/submissions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        Assertions.assertEquals(jobsBefore, executionJobRepository.count());
        Assertions.assertEquals(submissionsBefore, submissionRepository.count());
    }

    @Test
    @DisplayName("제출 회원은 세션으로 결정하며 제출과 채점 결과를 본인에게만 공개합니다.")
    void findSubmission_success_onlyForOwner() throws Exception {
        // given / when
        JsonNode response = jsonMapper.readTree(mockMvc.perform(post("/api/submissions")
                        .with(loginAs(memberId)).with(csrf())
                        .param("memberId", otherMemberId.toString())
                        .header("X-MEMBER-ID", otherMemberId.toString())
                        .contentType("application/json").content(submissionBody))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        UUID submissionId = UUID.fromString(response.path("submissionId").asString());
        UUID jobId = UUID.fromString(response.path("jobId").asString());
        entityManager.flush();
        entityManager.clear();

        // then
        Assertions.assertEquals(memberId, submissionRepository.findById(submissionId).orElseThrow().getMemberId());
        Assertions.assertEquals(memberId, executionJobRepository.findById(jobId).orElseThrow().getMemberId());
        mockMvc.perform(get("/api/submissions/" + submissionId).with(loginAs(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("private submission code"));
        mockMvc.perform(get("/api/jobs/" + jobId).with(loginAs(memberId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/submissions/" + submissionId).with(loginAs(otherMemberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/jobs/" + jobId).with(loginAs(otherMemberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/jobs/" + jobId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/submissions").with(loginAs(memberId)).param("exerciseId", exercise.getId()).param("language", "c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/submissions").with(loginAs(otherMemberId)).param("exerciseId", exercise.getId()).param("language", "c").param("memberId", memberId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("기존 비회원 제출은 보존하되 회원 기록과 채점 결과에서 제외합니다.")
    void findSubmission_fail_withLegacyAnonymousSubmission() throws Exception {
        // given
        JsonNode response = jsonMapper.readTree(mockMvc.perform(post("/api/submissions").with(loginAs(memberId)).with(csrf()).contentType("application/json").content(submissionBody))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        UUID submissionId = UUID.fromString(response.path("submissionId").asString());
        UUID jobId = UUID.fromString(response.path("jobId").asString());
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE submission SET member_id = NULL WHERE id = :id").setParameter("id", submissionId).executeUpdate();
        entityManager.createNativeQuery("UPDATE execution_job SET member_id = NULL WHERE id = :id").setParameter("id", jobId).executeUpdate();
        entityManager.clear();

        // when / then
        mockMvc.perform(get("/api/submissions").with(loginAs(memberId)).param("exerciseId", exercise.getId()).param("language", "c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/submissions/" + submissionId).with(loginAs(memberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/jobs/" + jobId).with(loginAs(memberId)))
                .andExpect(status().isNotFound());
        Assertions.assertTrue(submissionRepository.existsById(submissionId));
        Assertions.assertTrue(executionJobRepository.existsById(jobId));
    }

    private OAuth2LoginRequestPostProcessor loginAs(UUID id) {
        DefaultOAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OIDC_USER")), Map.of("member_id", id.toString()), "member_id");
        return oauth2Login().oauth2User(principal);
    }
}
