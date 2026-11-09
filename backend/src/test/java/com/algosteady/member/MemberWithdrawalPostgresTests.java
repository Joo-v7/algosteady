package com.algosteady.member;

import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import com.algosteady.learning.repository.DailyLearningRepository;
import com.algosteady.member.repository.MemberRepository;
import com.algosteady.member.domain.Member;
import com.algosteady.member.service.MemberCommandService;
import com.algosteady.training.domain.Submission;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원탈퇴의 본인 데이터 삭제·세션 만료·후속 저장 차단 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class MemberWithdrawalPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExecutionJobRepository executionJobRepository;

    @Autowired
    private ExecutionTestResultRepository executionTestResultRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DailyLearningRepository dailyLearningRepository;

    @Autowired
    private DailyLearningCompletionRepository dailyLearningCompletionRepository;

    @Autowired
    private JudgingCommandService judgingCommandService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MockMvc mockMvc;
    private UUID memberId;
    private UUID otherMemberId;
    private DefaultOAuth2User user;
    private final LocalDate date = LocalDate.of(2026, 8, 17);

    @BeforeEach
    void setUp() {
        memberId = memberCommandService.login("test", UUID.randomUUID().toString(), "withdraw@example.com", "탈퇴 회원").getId();
        otherMemberId = memberCommandService.login("test", UUID.randomUUID().toString(), "other@example.com", "다른 회원").getId();
        user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("member_id", memberId.toString()), "member_id");
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("탈퇴는 본인 제출·채점·학습 기록을 삭제하고 다른 회원·공통 배정·공개 실행을 유지합니다.")
    void withdraw_success_deletesOnlyOwnedData() throws Exception {
        // given
        ExecutionJob completed = saveGrading(memberId, ExecutionJob.Status.COMPLETED);
        ExecutionJob queued = saveGrading(memberId, ExecutionJob.Status.QUEUED);
        ExecutionJob running = saveGrading(memberId, ExecutionJob.Status.RUNNING);
        ExecutionJob otherJob = saveGrading(otherMemberId, ExecutionJob.Status.COMPLETED);
        dailyLearningRepository.assign(date, "recursion-factorial");
        dailyLearningCompletionRepository.complete(memberId, date, completed.getId(), Instant.now());
        dailyLearningCompletionRepository.complete(otherMemberId, date, otherJob.getId(), Instant.now());
        ExecutionJob publicRun = executionJobRepository.save(ExecutionJob.create("recursion-factorial", "1.0.0", "c", "code", "example"));
        MockHttpSession currentSession = new MockHttpSession();
        MockHttpSession otherSession = new MockHttpSession();
        sessionRegistry.registerNewSession(currentSession.getId(), user);
        sessionRegistry.registerNewSession(otherSession.getId(), user);
        entityManager.flush();
        entityManager.clear();

        // when
        mockMvc.perform(delete("/api/auth/me").session(currentSession).with(oauth2Login().oauth2User(user)).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("ALGOSTEADY_SESSION", 0));
        entityManager.clear();

        // then
        Assertions.assertFalse(memberRepository.existsById(memberId));
        for (ExecutionJob job : List.of(completed, queued, running)) {
            Assertions.assertFalse(executionJobRepository.existsById(job.getId()));
            Assertions.assertTrue(executionTestResultRepository.findAllByJobId(job.getId()).isEmpty());
            Assertions.assertTrue(submissionRepository.findLinksByMemberIdAndJobIds(memberId, List.of(job.getId())).isEmpty());
        }
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, date).isEmpty());
        Assertions.assertTrue(memberRepository.existsById(otherMemberId));
        Assertions.assertTrue(executionJobRepository.existsById(otherJob.getId()));
        Assertions.assertFalse(executionTestResultRepository.findAllByJobId(otherJob.getId()).isEmpty());
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(otherMemberId, date).isPresent());
        Assertions.assertTrue(executionJobRepository.existsById(publicRun.getId()));
        Assertions.assertTrue(dailyLearningRepository.existsById(date));
        Assertions.assertTrue(currentSession.isInvalid());
        Assertions.assertTrue(sessionRegistry.getSessionInformation(otherSession.getId()).isExpired());
        mockMvc.perform(get("/api/auth/me").session(otherSession).with(oauth2Login().oauth2User(user))).andExpect(status().isUnauthorized());
        sessionRegistry.removeSessionInformation(currentSession.getId());
        sessionRegistry.removeSessionInformation(otherSession.getId());
    }

    @Test
    @DisplayName("탈퇴 후 도착한 채점 콜백은 기록을 다시 만들지 않습니다.")
    void withdraw_success_ignoresLateCallbacks() {
        // given
        ExecutionJob job = saveGrading(memberId, ExecutionJob.Status.RUNNING);
        entityManager.flush();
        memberCommandService.withdraw(memberId);
        entityManager.clear();
        TestExecutionResult test = new TestExecutionResult("test", TestExecutionResult.Status.COMPLETED, "OK", "", 0, 1L, null);

        // when
        judgingCommandService.saveTest(job.getId(), test, jsonMapper.readTree("{\"stdout\":\"OK\",\"exitCode\":0,\"stderrRequired\":false}"));
        judgingCommandService.complete(job.getId(), new ExecutionResult(new PreparationResult(PreparationResult.Status.SUCCEEDED, "", "", 0, null), List.of(test)));
        judgingCommandService.fail(job.getId(), "late failure");
        entityManager.flush();
        entityManager.clear();

        // then
        Assertions.assertFalse(executionJobRepository.existsById(job.getId()));
        Assertions.assertTrue(executionTestResultRepository.findAllByJobId(job.getId()).isEmpty());
    }

    @Test
    @DisplayName("탈퇴한 회원의 남은 인증으로 새 제출을 등록할 수 없습니다.")
    void register_fail_afterWithdrawal() throws Exception {
        // given
        memberCommandService.withdraw(memberId);
        entityManager.clear();
        SubmissionRequestDto request = SubmissionRequestDto.builder()
                .exerciseId("recursion-factorial")
                .contentVersion("1.0.0")
                .language("c")
                .code("code")
                .referenceViewed(false)
                .build();

        // when / then
        mockMvc.perform(post("/api/submissions").with(oauth2Login().oauth2User(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 또는 CSRF 없이 탈퇴할 수 없고 계정은 유지됩니다.")
    void withdraw_fail_withoutAuthenticationOrCsrf() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/auth/me").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/me").with(oauth2Login().oauth2User(user))).andExpect(status().isForbidden());
        Assertions.assertTrue(memberRepository.existsById(memberId));
    }

    @Test
    @DisplayName("같은 제공자 계정으로 다시 로그인해도 이전 회원과 기록은 복구되지 않습니다.")
    void login_success_createsNewIdentityAfterWithdrawal() {
        // given
        Member before = memberCommandService.login("google", "withdraw-rejoin", "rejoin@example.com", "재로그인 회원");
        saveGrading(before.getId(), ExecutionJob.Status.COMPLETED);
        entityManager.flush();
        memberCommandService.withdraw(before.getId());
        entityManager.clear();

        // when
        Member after = memberCommandService.login("google", "withdraw-rejoin", "rejoin@example.com", "재로그인 회원");

        // then
        Assertions.assertNotEquals(before.getId(), after.getId());
        Assertions.assertFalse(memberRepository.existsById(before.getId()));
        Assertions.assertTrue(dailyLearningCompletionRepository.findByMemberIdAndLearningDate(after.getId(), date).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("진행 중인 결과 저장이 끝난 뒤 탈퇴하며 삭제 롤백 시 원래 기록을 유지합니다.")
    void withdraw_success_serializesResultStorageAndRollsBackAtomically() throws Exception {
        // given
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutionJob job = transaction.execute(status -> saveGrading(memberId, ExecutionJob.Status.RUNNING));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            // 삭제 후 예외 발생 시 계정·제출·채점 결과 전체 롤백
            Assertions.assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                memberCommandService.withdraw(memberId);
                throw new IllegalStateException("rollback");
            }));
            Assertions.assertTrue(memberRepository.existsById(memberId));
            Assertions.assertTrue(executionJobRepository.existsById(job.getId()));
            Assertions.assertFalse(executionTestResultRepository.findAllByJobId(job.getId()).isEmpty());

            Future<?> writer = executor.submit(() -> transaction.executeWithoutResult(status -> {
                memberRepository.findByIdForUpdate(memberId).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("결과 저장 대기 시간 초과");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                TestExecutionResult result = new TestExecutionResult("concurrent", TestExecutionResult.Status.COMPLETED, "OK", "", 0, 1L, null);
                judgingCommandService.saveTest(job.getId(), result, jsonMapper.readTree("{\"stdout\":\"OK\",\"exitCode\":0,\"stderrRequired\":false}"));
            }));
            Assertions.assertTrue(locked.await(5, TimeUnit.SECONDS));

            // when
            Future<?> withdrawal = executor.submit(() -> memberCommandService.withdraw(memberId));
            Assertions.assertThrows(TimeoutException.class, () -> withdrawal.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            writer.get(5, TimeUnit.SECONDS);
            withdrawal.get(5, TimeUnit.SECONDS);

            // then
            Assertions.assertFalse(memberRepository.existsById(memberId));
            Assertions.assertFalse(executionJobRepository.existsById(job.getId()));
            Assertions.assertTrue(executionTestResultRepository.findAllByJobId(job.getId()).isEmpty());
        } finally {
            release.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            for (UUID id : List.of(memberId, otherMemberId)) {
                if (memberRepository.existsById(id)) {
                    memberCommandService.withdraw(id);
                }
            }
        }
    }

    private ExecutionJob saveGrading(UUID owner, ExecutionJob.Status status) {
        ExecutionJob job = ExecutionJob.createGrading(owner, "recursion-factorial", "1.0.0", "c", "code", Instant.now());
        if (status == ExecutionJob.Status.RUNNING) {
            job.markRunning();
        } else if (status == ExecutionJob.Status.COMPLETED) {
            job.complete("{}", false);
        }
        executionJobRepository.save(job);
        submissionRepository.save(Submission.create(owner, "recursion-factorial", "1.0.0", "c", "code", false, job.getId(), job.getQueuedAt()));
        executionTestResultRepository.save(ExecutionTestResult.create(job.getId(), "test", "{}", true));
        return job;
    }
}
