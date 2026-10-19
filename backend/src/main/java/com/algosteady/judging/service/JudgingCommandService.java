package com.algosteady.judging.service;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.domain.ExecutionTestResult;
import com.algosteady.judging.repository.*;
import com.algosteady.judging.dto.request.RunRequestDto;
import com.algosteady.judging.dto.response.RunResponseDto;
import com.algosteady.judging.runner.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

/** 실행 요청 등록 및 작업 상태 변경 */
@Service
@RequiredArgsConstructor
@Transactional
public class JudgingCommandService {

    private final ExecutionJobRepository executionJobRepository;

    private final ExecutionTestResultRepository executionTestResultRepository;

    private final ExerciseQueryService exerciseQueryService;

    private final JsonMapper jsonMapper;

    /**
     * 서버 콘텐츠에서 공개 예시를 검증한 뒤 작업을 저장한다.
     * @param request 실행할 코드와 공개 예시 정보
     * @return 등록된 실행 작업 ID
     */
    public RunResponseDto register(RunRequestDto request) {
        JsonNode snapshot = exerciseQueryService.findSnapshot(request.getExerciseId(), request.getContentVersion(), request.getLanguage());
        exerciseQueryService.findPublicExample(snapshot, request.getLanguage(), request.getExampleId());
        ExecutionJob job = ExecutionJob.create(request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), request.getExampleId());
        executionJobRepository.save(job);

        return RunResponseDto.builder()
                .jobId(job.getId())
                .build();
    }

    /**
     * 지정 버전의 전체 테스트를 실행할 채점 작업을 저장한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @param code 사용자가 작성한 코드 원본
     * @return 생성한 채점 작업 ID
     */
    public UUID registerGrading(String exerciseId, String contentVersion, String language, String code) {
        exerciseQueryService.findSnapshot(exerciseId, contentVersion, language);
        ExecutionJob job = ExecutionJob.createGrading(exerciseId, contentVersion, language, code);
        executionJobRepository.save(job);

        return job.getId();
    }

    /**
     * 가장 오래된 대기 작업을 실행 상태로 확정한다.
     * @return 실행 상태로 변경한 작업, 대기 작업이 없으면 null
     */
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
     * 실제 출력과 콘텐츠의 기대값을 비교해 저장한다.
     * @param jobId 실행 또는 채점 작업 ID
     * @param result 테스트 하나의 실제 실행 결과
     * @param expected 콘텐츠에 정의된 기대 출력과 종료 조건
     */
    public void saveTest(UUID jobId, TestExecutionResult result, JsonNode expected) {
        String actualStdout = result.stdout().replace("\r\n", "\n").replace('\r', '\n').replaceFirst("\n$", "");
        String expectedStdout = expected.path("stdout").asString().replace("\r\n", "\n").replace('\r', '\n').replaceFirst("\n$", "");
        boolean passed = result.status() == TestExecutionResult.Status.COMPLETED
                && expectedStdout.equals(actualStdout)
                && Integer.valueOf(expected.path("exitCode").asInt()).equals(result.exitCode())
                && expected.path("stderrRequired").asBoolean() == !result.stderr().isBlank();
        executionTestResultRepository.save(ExecutionTestResult.create(jobId, result.testId(), jsonMapper.writeValueAsString(result), passed));
    }

    /**
     * 컴파일 결과와 환경 장애 여부로 최종 상태를 저장한다.
     * @param jobId 실행 또는 채점 작업 ID
     * @param result 컴파일 결과와 테스트별 실행 결과
     */
    public void complete(UUID jobId, ExecutionResult result) {
        boolean environmentError = result.preparation().status() == PreparationResult.Status.ENVIRONMENT_ERROR
                || result.tests().stream()
                        .anyMatch(test -> test.status() == TestExecutionResult.Status.ENVIRONMENT_ERROR);
        executionJobRepository.findById(jobId).orElseThrow().complete(jsonMapper.writeValueAsString(result.preparation()), environmentError);
    }

    /**
     * 작업을 시스템 장애로 종료한다.
     * @param jobId 실행 또는 채점 작업 ID
     * @param message 작업 실패 사유
     */
    public void fail(UUID jobId, String message) {
        executionJobRepository.findById(jobId).orElseThrow().markFailed(message);
    }

    /**
     * 재시작 전에 실행 중이던 작업을 중단으로 기록한다.
     */
    public void recoverInterrupted() {
        for (ExecutionJob job : executionJobRepository.findAllByStatus(ExecutionJob.Status.RUNNING)) {
            job.markFailed("서버 재시작으로 실행이 중단되었습니다.");
        }
    }
}
