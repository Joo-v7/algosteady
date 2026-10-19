package com.algosteady.judging.service;

import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.ExecutionJob;
import com.algosteady.judging.runner.service.CodeExecutionService;
import com.algosteady.judging.runner.language.LanguageSpec;
import com.algosteady.judging.runner.model.*;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.*;

/** DB 대기 작업의 순차 실행 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionWorker {

    private final JudgingCommandService judgingCommandService;

    private final ExerciseQueryService exerciseQueryService;

    private final CodeExecutionService codeExecutionService;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * 콘텐츠 등록 후 중단된 작업을 복구하고 큐 처리를 시작한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        judgingCommandService.recoverInterrupted();
        scheduledExecutorService.scheduleWithFixedDelay(this::processNext, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void processNext() {
        ExecutionJob job = null;

        try {
            job = judgingCommandService.claimNext();

            if (job == null) {
                return;
            }

            JsonNode snapshot = exerciseQueryService.findSnapshot(job.getExerciseId(), job.getContentVersion(), job.getLanguage());
            JsonNode source = snapshot.path("sources").path(job.getLanguage());
            Map<String, JsonNode> selectedTests = new LinkedHashMap<>();

            if (job.getKind() == ExecutionJob.Kind.RUN) {
                JsonNode example = exerciseQueryService.findPublicExample(snapshot, job.getLanguage(), job.getExampleId());
                selectedTests.put(job.getExampleId(), example);
            } else {
                for (JsonNode test : source.path("tests")) {
                    selectedTests.put(test.path("id").asString(), test);
                }
            }

            List<TestInput> inputs = new ArrayList<>();
            selectedTests.forEach((id, test) -> inputs.add(new TestInput(id, test.path("input").asString())));
            ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, job.getCode(), source.path("harness").asString(), inputs);
            UUID jobId = job.getId();
            // 실행 중에는 DB 트랜잭션을 유지하지 않는다.
            ExecutionResult result = codeExecutionService.execute(request, test -> judgingCommandService.saveTest(jobId, test, selectedTests.get(test.testId()).path("expected")));
            judgingCommandService.complete(jobId, result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            if (job != null) {
                recordFailure(job, "서버 종료로 실행이 중단되었습니다.");
            }
        } catch (Exception exception) {
            log.error("실행 작업 처리 중 오류가 발생했습니다.", exception);

            if (job != null) {
                recordFailure(job, "실행 환경 또는 서버 장애로 작업이 중단되었습니다.");
            }
        }
    }

    private void recordFailure(ExecutionJob job, String message) {
        try {
            judgingCommandService.fail(job.getId(), message);
        } catch (RuntimeException exception) {
            log.error("작업 중단 상태를 저장하지 못했습니다: {}", job.getId(), exception);
        }
    }

    /**
     * 실행 스레드를 중단하고 runner의 자원 정리를 기다립니다.
     */
    @PreDestroy
    public void stop() {
        scheduledExecutorService.shutdownNow();

        try {
            if (!scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("실행 작업자 종료 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
