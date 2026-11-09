package com.algosteady.learning;

import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import com.algosteady.learning.repository.DailyLearningRepository;
import com.algosteady.member.service.MemberCommandService;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.repository.SubmissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 PostgreSQL 기반 학습 현황·연도별 기록·회원 격리 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class LearningHistoryPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DailyLearningRepository dailyLearningRepository;

    @Autowired
    private DailyLearningCompletionRepository dailyLearningCompletionRepository;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private UUID memberId;
    private UUID otherMemberId;
    private final Instant now = Instant.parse("2028-01-02T21:00:00Z");

    @BeforeEach
    void setUp() {
        memberId = memberCommandService.login("test", UUID.randomUUID().toString(), "history@example.com", "학습 기록 회원").getId();
        otherMemberId = memberCommandService.login("test", UUID.randomUUID().toString(), "other@example.com", "다른 회원").getId();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("완료 이력이 없으면 현황은 0이고 날짜 목록은 비어 있습니다.")
    void findHistory_success_withNoCompletions() throws Exception {
        // when / then
        findHistory(now, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningDate").value("2028-01-03"))
                .andExpect(jsonPath("$.completedToday").value(false))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.totalCompletedDays").value(0))
                .andExpect(jsonPath("$.days").isEmpty());
    }

    @Test
    @DisplayName("연속 일수는 연말을 넘어 계산하고 선택 연도의 본인 제출만 연결합니다.")
    void findHistory_success_withYearBoundaryAndMemberIsolation() throws Exception {
        // given
        UUID latestSubmission = saveCompletion(memberId, LocalDate.of(2028, 1, 2));
        saveCompletion(memberId, LocalDate.of(2028, 1, 1));
        saveCompletion(memberId, LocalDate.of(2027, 12, 31));
        saveCompletion(memberId, LocalDate.of(2027, 12, 29));
        saveCompletion(otherMemberId, LocalDate.of(2028, 1, 3));
        entityManager.flush();
        entityManager.clear();

        // when / then
        findHistory(now, "?year=2028")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedToday").value(false))
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.totalCompletedDays").value(4))
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[0].learningDate").value("2028-01-02"))
                .andExpect(jsonPath("$.days[0].submissionId").value(latestSubmission.toString()));
        findHistory(now, "?year=2027")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.totalCompletedDays").value(4))
                .andExpect(jsonPath("$.days.length()").value(2));
    }

    @ParameterizedTest
    @CsvSource({"2028-01-02T20:59:59.999999Z,2028-01-02,true,1", "2028-01-02T21:00:00Z,2028-01-03,false,1", "2028-01-03T21:00:00Z,2028-01-04,false,0"})
    @DisplayName("오전 6시 전후로 오늘 완료와 연속 일수의 유효 기간을 구분합니다.")
    void findHistory_success_withLearningDateBoundary(String timestamp, String date, boolean completed, int streak) throws Exception {
        // given
        saveCompletion(memberId, LocalDate.of(2028, 1, 2));

        // when / then
        findHistory(Instant.parse(timestamp), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningDate").value(date))
                .andExpect(jsonPath("$.completedToday").value(completed))
                .andExpect(jsonPath("$.currentStreak").value(streak));
    }

    @Test
    @DisplayName("윤년 완료 날짜를 보존하고 미래 날짜를 현황에서 제외합니다.")
    void findHistory_success_withLeapDayAndFutureRecord() throws Exception {
        // given
        saveCompletion(memberId, LocalDate.of(2028, 2, 29));
        saveCompletion(memberId, LocalDate.of(2028, 3, 2));

        // when / then
        findHistory(Instant.parse("2028-02-29T21:00:00Z"), "?year=2028")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.totalCompletedDays").value(1))
                .andExpect(jsonPath("$.days[0].learningDate").value("2028-02-29"));
    }

    @ParameterizedTest
    @CsvSource({"0", "2029", "not-a-year"})
    @DisplayName("유효하지 않은 연도는 400으로 거절합니다.")
    void findHistory_fail_withInvalidYear(String year) throws Exception {
        // when / then
        findHistory(now, "?year=" + year).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인하지 않은 학습 기록 조회는 401로 거절합니다.")
    void findHistory_fail_withoutLogin() throws Exception {
        // when / then
        mockMvc.perform(get("/api/learning/history")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "86399,true", "86400,false"})
    @DisplayName("오늘의 학습 제출 여부는 오전 6시 이상 다음 날 오전 6시 미만으로 구분합니다.")
    void findToday_success_withSubmissionBoundaries(long offsetSeconds, boolean attempted) throws Exception {
        // given
        LocalDate date = LocalDate.of(2028, 1, 3);
        dailyLearningRepository.assign(date, "recursion-factorial");
        Instant acceptedAt = now.plusSeconds(offsetSeconds);
        ExecutionJob job = ExecutionJob.createGrading(memberId, "recursion-factorial", "1.0.0", "c", "code", acceptedAt);
        executionJobRepository.save(job);
        submissionRepository.save(Submission.create(memberId, "recursion-factorial", "1.0.0", "c", "code", false, job.getId(), acceptedAt));
        entityManager.flush();
        entityManager.clear();
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("member_id", memberId.toString()), "member_id");

        // when / then
        try (MockedStatic<Instant> clock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(now);
            mockMvc.perform(get("/api/learning/today").with(oauth2Login().oauth2User(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attempted").value(attempted))
                    .andExpect(jsonPath("$.completed").value(false));
            mockMvc.perform(get("/api/learning/today"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attempted").value(false));
        }
    }

    @Test
    @DisplayName("다른 과제·다른 회원의 제출과 예제 실행은 본인의 진행 상태를 만들지 않습니다.")
    void findToday_success_excludesUnrelatedAttempts() throws Exception {
        // given
        dailyLearningRepository.assign(LocalDate.of(2028, 1, 3), "recursion-factorial");
        ExecutionJob otherMemberJob = ExecutionJob.createGrading(otherMemberId, "recursion-factorial", "1.0.0", "c", "code", now);
        ExecutionJob otherExerciseJob = ExecutionJob.createGrading(memberId, "recursion-power", "1.0.0", "c", "code", now);
        for (ExecutionJob job : List.of(otherMemberJob, otherExerciseJob)) {
            executionJobRepository.save(job);
            submissionRepository.save(Submission.create(job.getMemberId(), job.getExerciseId(), "1.0.0", "c", "code", false, job.getId(), now));
        }
        executionJobRepository.save(ExecutionJob.create("recursion-factorial", "1.0.0", "c", "code", "example"));
        entityManager.flush();
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("member_id", memberId.toString()), "member_id");

        // when / then
        try (MockedStatic<Instant> clock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(now);
            mockMvc.perform(get("/api/learning/today").with(oauth2Login().oauth2User(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attempted").value(false));
        }
    }

    private ResultActions findHistory(Instant timestamp, String query) throws Exception {
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("member_id", memberId.toString()), "member_id");
        try (MockedStatic<Instant> clock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(timestamp);
            return mockMvc.perform(get("/api/learning/history" + query).with(oauth2Login().oauth2User(user)));
        }
    }

    private UUID saveCompletion(UUID ownerId, LocalDate date) {
        Instant acceptedAt = date.atTime(6, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        dailyLearningRepository.assign(date, "recursion-factorial");
        ExecutionJob job = ExecutionJob.createGrading(ownerId, "recursion-factorial", "1.0.0", "c", "code", acceptedAt);
        job.complete("{}", false);
        executionJobRepository.save(job);
        Submission submission = Submission.create(ownerId, "recursion-factorial", "1.0.0", "c", "code", false, job.getId(), acceptedAt);
        submissionRepository.save(submission);
        dailyLearningCompletionRepository.complete(ownerId, date, job.getId(), acceptedAt.plusSeconds(1));
        return submission.getId();
    }
}
