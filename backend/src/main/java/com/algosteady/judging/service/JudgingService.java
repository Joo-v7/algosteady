package com.algosteady.judging.service;

import com.algosteady.exercise.service.ExerciseService;
import com.algosteady.judging.domain.CriterionProgress;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.domain.GradingPassedEvent;
import com.algosteady.judging.dto.request.RunRequestDto;
import com.algosteady.judging.dto.response.CriterionResultResponseDto;
import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.judging.dto.response.RunResponseDto;
import com.algosteady.judging.dto.response.TestResultResponseDto;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.repository.ExecutionTestSummary;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.member.repository.MemberRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 실행·채점 작업 등록 및 결과 조회
 *
 * @author Joo
 * @since 2026.11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JudgingService {

    private final ExecutionJobRepository executionJobRepository;
    private final ExecutionTestResultRepository executionTestResultRepository;
    private final ExerciseService exerciseService;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MemberRepository memberRepository;

    /**
     * 서버 콘텐츠에서 공개 예시를 검증한 뒤 작업 저장
     * @param request 실행할 코드와 공개 예시 정보
     * @return 등록된 실행 작업 ID
     */
    @Transactional
    public RunResponseDto register(RunRequestDto request) {
        JsonNode snapshot = exerciseService.findSnapshot(request.getExerciseId(), request.getContentVersion(), request.getLanguage());
        exerciseService.findPublicExample(snapshot, request.getLanguage(), request.getExampleId());
        ExecutionJob job = ExecutionJob.create(request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), request.getExampleId());
        executionJobRepository.save(job);

        return RunResponseDto.builder()
                .jobId(job.getId())
                .build();
    }

    /**
     * 지정 버전의 전체 테스트를 실행할 채점 작업 저장
     * @param memberId 제출한 회원 ID
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @param code 사용자가 작성한 코드 원본
     * @param acceptedAt 서버에서 제출을 접수한 시각
     * @return 생성한 채점 작업 ID
     */
    @Transactional
    public UUID registerGrading(UUID memberId, String exerciseId, String contentVersion, String language, String code, Instant acceptedAt) {
        exerciseService.findSnapshot(exerciseId, contentVersion, language);
        ExecutionJob job = ExecutionJob.createGrading(memberId, exerciseId, contentVersion, language, code, acceptedAt);
        executionJobRepository.save(job);

        return job.getId();
    }

    /**
     * 가장 오래된 대기 작업을 실행 상태로 확정
     * @return 실행 상태로 변경한 작업, 대기 작업이 없으면 null
     */
    @Transactional
    public ExecutionJob claimNext() {
        Optional<ExecutionJob> next = executionJobRepository.findFirstByStatusOrderByQueuedAtAscIdAsc(ExecutionJob.Status.QUEUED);

        if (next.isEmpty()) {
            return null;
        }

        ExecutionJob job = next.get();
        job.markRunning();

        return job;
    }

    /**
     * 실제 출력과 콘텐츠의 기대값을 비교해 저장
     * @param jobId 실행 또는 채점 작업 ID
     * @param result 테스트 하나의 실제 실행 결과
     * @param expected 콘텐츠에 정의된 기대 출력과 종료 조건
     */
    @Transactional
    public void saveTest(UUID jobId, TestExecutionResult result, JsonNode expected) {
        if (findExistingJob(jobId) == null) {
            return;
        }

        // 줄바꿈 형식 통일 및 마지막 줄바꿈 한 개 제외
        String actualStdout = result.stdout().replace("\r\n", "\n").replace('\r', '\n').replaceFirst("\n$", "");
        String expectedStdout = expected.path("stdout").asString().replace("\r\n", "\n").replace('\r', '\n').replaceFirst("\n$", "");
        // 실행 상태·표준 출력·종료 코드·오류 출력 여부 비교
        boolean passed = result.status() == TestExecutionResult.Status.COMPLETED
                && expectedStdout.equals(actualStdout)
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(result.exitCode())
                && expected.path("stderrRequired").asBoolean() == !result.stderr().isBlank();
        // 테스트별 실제 결과와 통과 여부 저장
        executionTestResultRepository.save(ExecutionTestResult.create(jobId, result.testId(), jsonMapper.writeValueAsString(result), passed));
    }

    /**
     * 컴파일 결과와 환경 장애 여부로 최종 상태 저장
     * @param jobId 실행 또는 채점 작업 ID
     * @param result 컴파일 결과와 테스트별 실행 결과
     */
    @Transactional
    public void complete(UUID jobId, ExecutionResult result) {
        ExecutionJob job = findExistingJob(jobId);
        if (job == null) {
            return;
        }

        // 컴파일·프로그램 오류와 실행 환경 장애 구분
        boolean environmentError = result.preparation().status() == PreparationResult.Status.ENVIRONMENT_ERROR
                || result.tests().stream()
                        .anyMatch(test -> test.status() == TestExecutionResult.Status.ENVIRONMENT_ERROR);
        job.complete(jsonMapper.writeValueAsString(result.preparation()), environmentError);

        if (job.getKind() != ExecutionJob.Kind.GRADING || job.getMemberId() == null || environmentError
                || result.preparation().status() != PreparationResult.Status.SUCCEEDED) {
            return;
        }

        // 상세·기록 조회와 같은 필수 테스트 집계로 전체 기준 통과 확인
        JsonNode snapshot = exerciseService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
        JsonNode tests = snapshot.path("sources").path(job.getLanguage()).path("tests");
        JsonNode criteria = snapshot.path("exercise").path("criteria");
        Set<String> passedTestIds = executionTestResultRepository.findAllByJobId(jobId).stream()
                .filter(ExecutionTestResult::isPassed)
                .map(ExecutionTestResult::getTestId)
                .collect(Collectors.toSet());
        if (criteria.isEmpty()) {
            return;
        }
        for (JsonNode criterion : criteria) {
            CriterionProgress progress = CriterionProgress.calculate(criterion.path("id").asString(), tests, passedTestIds);
            if (progress.testIds().isEmpty() || !progress.isPassed()) {
                return;
            }
        }

        applicationEventPublisher.publishEvent(new GradingPassedEvent(jobId, job.getMemberId(), job.getExerciseId(), job.getQueuedAt(), job.getCompletedAt()));
    }

    /**
     * 작업을 시스템 장애로 종료
     * @param jobId 실행 또는 채점 작업 ID
     * @param message 작업 실패 사유
     */
    @Transactional
    public void fail(UUID jobId, String message) {
        ExecutionJob job = findExistingJob(jobId);
        if (job != null) {
            job.markFailed(message);
        }
    }

    /**
     * 탈퇴 삭제와 결과 저장 사이의 회원 잠금 및 삭제된 작업 제외
     * @param jobId 실행·채점 작업 ID
     * @return 회원과 작업이 남아 있으면 해당 작업, 삭제된 경우 null
     */
    private ExecutionJob findExistingJob(UUID jobId) {
        Optional<ExecutionJob> job = executionJobRepository.findById(jobId);
        if (job.isEmpty()) {
            return null;
        }
        UUID memberId = job.get().getMemberId();
        if (memberId != null && memberRepository.findByIdForUpdate(memberId).isEmpty()) {
            return null;
        }
        return job.get();
    }

    /**
     * 재시작 전에 실행 중이던 작업을 중단으로 기록
     */
    @Transactional
    public void recoverInterrupted() {
        for (ExecutionJob job : executionJobRepository.findAllByStatus(ExecutionJob.Status.RUNNING)) {
            job.markFailed("서버 재시작으로 실행이 중단되었습니다.");
        }
    }

    /**
     * 작업 상태와 해당 콘텐츠 버전의 기대값 반환
     * @param jobId 실행 또는 채점 작업 ID
     * @param memberId 인증된 회원 ID, 비로그인은 null
     * @return 작업 상태와 테스트·기준별 판정 결과
     */
    public JobResponseDto findById(UUID jobId, UUID memberId) {
        // 작업 원본과 실행 당시 콘텐츠 조회
        ExecutionJob job = executionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "실행 작업을 찾을 수 없습니다."));

        // 공개 예제 실행은 허용하고 채점 작업은 제출한 회원만 조회
        if (job.getKind() == ExecutionJob.Kind.GRADING) {
            if (memberId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }
            if (!memberId.equals(job.getMemberId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "실행 작업을 찾을 수 없습니다.");
            }
        }

        JsonNode snapshot = exerciseService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
        boolean grading = job.getKind() == ExecutionJob.Kind.GRADING;
        List<JsonNode> selectedTests = new ArrayList<>();

        if (grading) {
            snapshot.path("sources").path(job.getLanguage()).path("tests").forEach(selectedTests::add);
        } else {
            selectedTests.add(exerciseService.findPublicExample(snapshot, job.getLanguage(), job.getExampleId()));
        }

        // 저장된 실행 결과를 테스트 ID와 연결
        Map<String, ExecutionTestResult> resultsByTestId = executionTestResultRepository.findAllByJobId(jobId).stream()
                .collect(Collectors.toMap(ExecutionTestResult::getTestId, Function.identity()));
        Set<String> passedTestIds = resultsByTestId.values().stream()
                .filter(ExecutionTestResult::isPassed)
                .map(ExecutionTestResult::getTestId)
                .collect(Collectors.toSet());
        Map<String, JsonNode> actualResultsByTestId = resultsByTestId.values().stream()
                .collect(Collectors.toMap(ExecutionTestResult::getTestId, result -> jsonMapper.readTree(result.getResult())));
        List<TestResultResponseDto> tests = new ArrayList<>();
        String pendingCriterionId = null;

        // 선택한 테스트의 입력·기대값·실제 결과 연결
        for (JsonNode test : selectedTests) {
            ExecutionTestResult result = resultsByTestId.get(test.path("id").asString());

            if (result == null) {
                if (pendingCriterionId == null) {
                    pendingCriterionId = test.path("criterionId").asString();
                }
                continue;
            }
            tests.add(TestResultResponseDto.builder()
                    .testId(result.getTestId())
                    .criterionId(test.path("criterionId").asString())
                    .description(test.path("description").asString())
                    .input(test.path("input").asString())
                    .expected(test.path("expected"))
                    .actual(actualResultsByTestId.get(result.getTestId()))
                    .passed(result.isPassed())
                    .build());
        }

        // 저장된 테스트 결과와 작업 상태를 기준별 판정으로 집계
        List<CriterionResultResponseDto> criteria = new ArrayList<>();

        if (grading) {
            for (JsonNode criterion : snapshot.path("exercise").path("criteria")) {
                String criterionId = criterion.path("id").asString();
                CriterionProgress progress = CriterionProgress.calculate(criterionId, selectedTests, passedTestIds);
                List<ExecutionTestResult> criterionTests = resultsByTestId.values().stream()
                        .filter(result -> progress.testIds().contains(result.getTestId()))
                        .toList();
                // 실행 실패와 미실행 구분, 실패한 기준의 판정 우선 보존
                boolean unavailable = false;
                boolean failed = false;
                for (ExecutionTestResult testResult : criterionTests) {
                    String testStatus = actualResultsByTestId.get(testResult.getTestId()).path("status").asString();
                    if (testStatus.equals("NOT_EXECUTED") || testStatus.equals("ENVIRONMENT_ERROR")) {
                        unavailable = true;
                    } else if (!testResult.isPassed()) {
                        failed = true;
                    }
                }
                String status;

                if (failed) {
                    status = "FAILED";
                } else if (progress.isPassed()) {
                    status = "PASSED";
                } else if (unavailable || job.getStatus() == ExecutionJob.Status.FAILED || job.getStatus() == ExecutionJob.Status.COMPLETED) {
                    status = "UNAVAILABLE";
                } else if (job.getStatus() == ExecutionJob.Status.RUNNING && criterionId.equals(pendingCriterionId)) {
                    status = "RUNNING";
                } else {
                    status = "PENDING";
                }
                criteria.add(CriterionResultResponseDto.builder()
                        .criterionId(criterionId)
                        .title(criterion.path("title").asString())
                        .description(criterion.path("description").asString())
                        .status(status)
                        .totalTests(progress.testIds().size())
                        .completedTests(criterionTests.size())
                        .passedTests(progress.passedTests())
                        .build());
            }
        }

        // 실행 원본과 테스트·기준별 판정 응답 구성
        return JobResponseDto.builder()
                .jobId(job.getId())
                .exerciseId(job.getExerciseId())
                .exerciseTitle(snapshot.path("exercise").path("title").asString())
                .contentVersion(job.getContentVersion())
                .language(job.getLanguage())
                .code(job.getCode())
                .exampleId(job.getExampleId())
                .kind(job.getKind().name())
                .status(job.getStatus().name())
                .queuedAt(job.getQueuedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .preparation(job.getPreparation() == null ? null : jsonMapper.readTree(job.getPreparation()))
                .tests(tests)
                .criteria(criteria)
                .message(job.getMessage())
                .build();
    }

    /**
     * 기록 목록의 작업 상태·통과 기준 일괄 조회 (실행 출력 제외)
     * @param jobIds 요약을 조회할 작업 ID 목록
     * @return 작업 ID별 상태와 통과 기준 수
     */
    public Map<UUID, JobSummaryResponseDto> findSummariesByIds(List<UUID> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        // 작업별 통과 테스트 ID 연결, 실행 출력 조회 제외
        List<ExecutionTestSummary> testSummaries = executionTestResultRepository.findSummariesByJobIds(jobIds);
        Map<UUID, Set<String>> passedTestIdsByJobId = new HashMap<>();
        for (ExecutionTestSummary testSummary : testSummaries) {
            if (!testSummary.getPassed()) {
                continue;
            }
            Set<String> passedTestIds = passedTestIdsByJobId.get(testSummary.getJobId());
            if (passedTestIds == null) {
                passedTestIds = new HashSet<>();
                passedTestIdsByJobId.put(testSummary.getJobId(), passedTestIds);
            }
            passedTestIds.add(testSummary.getTestId());
        }

        Map<List<String>, JsonNode> snapshotsByContentKey = new HashMap<>();
        Map<UUID, JobSummaryResponseDto> summaries = new HashMap<>();

        // 동일 콘텐츠의 스냅샷 재사용 및 작업별 통과 기준 집계
        for (ExecutionJob job : executionJobRepository.findAllById(jobIds)) {
            List<String> contentKey = List.of(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
            JsonNode snapshot = snapshotsByContentKey.get(contentKey);

            if (snapshot == null) {
                snapshot = exerciseService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
                snapshotsByContentKey.put(contentKey, snapshot);
            }

            Set<String> passedTestIds = passedTestIdsByJobId.getOrDefault(job.getId(), Set.of());
            int passedCriteria = 0;
            JsonNode criteria = snapshot.path("exercise").path("criteria");

            for (JsonNode criterion : criteria) {
                CriterionProgress progress = CriterionProgress.calculate(criterion.path("id").asString(),
                        snapshot.path("sources").path(job.getLanguage()).path("tests"), passedTestIds);
                if (progress.isPassed()) {
                    passedCriteria++;
                }
            }
            summaries.put(job.getId(), JobSummaryResponseDto.builder()
                    .jobId(job.getId())
                    .status(job.getStatus().name())
                    .passedCriteria(passedCriteria)
                    .totalCriteria(criteria.size())
                    .build());
        }

        return summaries;
    }
}
