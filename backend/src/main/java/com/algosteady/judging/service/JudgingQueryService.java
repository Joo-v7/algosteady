package com.algosteady.judging.service;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.CriterionProgress;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.ExecutionJobRepository;
import com.algosteady.judging.repository.ExecutionTestResultRepository;
import com.algosteady.judging.repository.ExecutionTestSummary;
import com.algosteady.judging.dto.response.CriterionResultResponseDto;
import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.judging.dto.response.TestResultResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/** 실행 원본 및 작업 결과 조회 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JudgingQueryService {

    private final ExecutionJobRepository executionJobRepository;

    private final ExecutionTestResultRepository executionTestResultRepository;

    private final ExerciseQueryService exerciseQueryService;

    private final JsonMapper jsonMapper;

    /**
     * 작업 상태와 해당 콘텐츠 버전의 기대값 반환
     * @param jobId 실행 또는 채점 작업 ID
     * @return 작업 상태와 테스트·기준별 판정 결과
     */
    public JobResponseDto findById(UUID jobId) {
        // 작업 원본과 실행 당시 콘텐츠 조회
        ExecutionJob job = executionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "실행 작업을 찾을 수 없습니다."));
        JsonNode snapshot = exerciseQueryService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
        boolean grading = job.getKind() == ExecutionJob.Kind.GRADING;
        List<JsonNode> selectedTests = new ArrayList<>();

        if (grading) {
            snapshot.path("sources").path(job.getLanguage()).path("tests").forEach(selectedTests::add);
        } else {
            selectedTests.add(exerciseQueryService.findPublicExample(snapshot, job.getLanguage(), job.getExampleId()));
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
                snapshot = exerciseQueryService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
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
