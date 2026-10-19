package com.algosteady.judging.service;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.*;
import com.algosteady.judging.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
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
     * 작업 상태와 해당 콘텐츠 버전의 기대값을 반환한다.
     * @param jobId 실행 또는 채점 작업 ID
     * @return 작업 상태와 테스트·기준별 판정 결과
     */
    public JobResponseDto findById(UUID jobId) {
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

        Map<String, ExecutionTestResult> storedResults = executionTestResultRepository.findAllByJobId(jobId).stream()
                .collect(Collectors.toMap(com.algosteady.judging.domain.ExecutionTestResult::getTestId, Function.identity()));
        List<TestResultResponseDto> tests = new ArrayList<>();
        String pendingCriterionId = null;

        for (JsonNode test : selectedTests) {
            ExecutionTestResult result = storedResults.get(test.path("id").asString());

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
                    .actual(jsonMapper.readTree(result.getResult()))
                    .passed(result.isPassed())
                    .build());
        }

        // 판정은 저장된 결과와 작업 상태에서 계산하므로 재시작 후에도 동일하게 복구된다.
        List<CriterionResultResponseDto> criteria = new ArrayList<>();

        if (grading) {
            for (JsonNode criterion : snapshot.path("exercise").path("criteria")) {
                String criterionId = criterion.path("id").asString();
                int totalTests = (int) selectedTests.stream()
                        .filter(test -> criterionId.equals(test.path("criterionId").asString()))
                        .count();
                List<TestResultResponseDto> criterionTests = tests.stream()
                        .filter(test -> criterionId.equals(test.getCriterionId()))
                        .toList();
                int passedTests = (int) criterionTests.stream()
                        .filter(TestResultResponseDto::isPassed)
                        .count();
                boolean unavailable = criterionTests.stream()
                        .anyMatch(test -> Set.of("NOT_EXECUTED", "ENVIRONMENT_ERROR").contains(test.getActual().path("status").asString()));
                boolean failed = criterionTests.stream()
                        .anyMatch(test -> !test.isPassed()
                                && !Set.of("NOT_EXECUTED", "ENVIRONMENT_ERROR").contains(test.getActual().path("status").asString()));
                String status;

                if (failed) {
                    status = "FAILED";
                } else if (passedTests == totalTests) {
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
                        .totalTests(totalTests)
                        .completedTests(criterionTests.size())
                        .passedTests(passedTests)
                        .build());
            }
        }

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
     * 기록 목록의 작업을 일괄 조회하며 실행 출력은 읽지 않는다.
     * @param jobIds 요약을 조회할 작업 ID 목록
     * @return 작업 ID별 상태와 통과 기준 수
     */
    public Map<UUID, JobSummaryResponseDto> findSummariesByIds(List<UUID> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Set<String>> testSummaries = executionTestResultRepository.findSummariesByJobIds(jobIds).stream()
                .filter(com.algosteady.judging.repository.ExecutionTestSummary::getPassed)
                .collect(Collectors.groupingBy(com.algosteady.judging.repository.ExecutionTestSummary::getJobId,
                        Collectors.mapping(com.algosteady.judging.repository.ExecutionTestSummary::getTestId, Collectors.toSet())));
        Map<List<String>, JsonNode> snapshots = new HashMap<>();
        Map<UUID, JobSummaryResponseDto> summaries = new HashMap<>();

        for (ExecutionJob job : executionJobRepository.findAllById(jobIds)) {
            List<String> key = List.of(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
            JsonNode snapshot = snapshots.get(key);

            if (snapshot == null) {
                snapshot = exerciseQueryService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
                snapshots.put(key, snapshot);
            }

            Set<String> passedTestIds = testSummaries.getOrDefault(job.getId(), Set.of());
            int passedCriteria = 0;
            JsonNode criteria = snapshot.path("exercise").path("criteria");

            for (JsonNode criterion : criteria) {
                Set<String> requiredTestIds = new HashSet<>();

                for (JsonNode test : snapshot.path("sources").path(job.getLanguage()).path("tests")) {
                    if (criterion.path("id").asString().equals(test.path("criterionId").asString())) {
                        requiredTestIds.add(test.path("id").asString());
                    }
                }

                if (passedTestIds.containsAll(requiredTestIds)) {
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
