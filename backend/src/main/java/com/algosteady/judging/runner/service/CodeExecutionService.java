package com.algosteady.judging.runner.service;

import com.algosteady.judging.runner.infrastructure.DockerContainerManager;
import com.algosteady.judging.runner.infrastructure.DockerExecutionSession;
import com.algosteady.judging.runner.model.ExecutionRequest;
import com.algosteady.judging.runner.model.ExecutionResult;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.runner.model.TestInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 제출 코드 컴파일 및 테스트 순차 실행 */
@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final DockerContainerManager dockerContainerManager;

    /**
     * 제출 코드를 컴파일하고 테스트별 실행 결과 전달
     * @param executionRequest 언어별 제출 코드와 테스트 입력
     * @param onTestCompleted 각 테스트 완료 시 결과를 전달할 처리
     * @return 컴파일 결과와 테스트별 실행 결과
     */
    public ExecutionResult execute(ExecutionRequest executionRequest, Consumer<TestExecutionResult> onTestCompleted) throws IOException, InterruptedException {
        Objects.requireNonNull(executionRequest, "request");
        Objects.requireNonNull(onTestCompleted, "onTestCompleted");

        // 작업별 컨테이너에서 컴파일 또는 문법 검사 수행
        try (DockerExecutionSession executionSession = dockerContainerManager.open(executionRequest)) {
            PreparationResult preparationResult = executionSession.prepare();
            List<TestExecutionResult> testResults = new ArrayList<>();
            String unavailableReason = preparationResult.status() == PreparationResult.Status.SUCCEEDED ? null : "준비 단계가 완료되지 않아 실행하지 않았습니다.";

            // 테스트를 실행하고 완료 결과를 채점부에 전달
            for (TestInput testInput : executionRequest.tests()) {
                TestExecutionResult testResult;

                if (unavailableReason != null) {
                    testResult = new TestExecutionResult(testInput.id(), TestExecutionResult.Status.NOT_EXECUTED, "", "", null, null, unavailableReason);
                } else {
                    testResult = executionSession.execute(testInput);
                }
                testResults.add(testResult);
                onTestCompleted.accept(testResult);

                if (testResult.status() == TestExecutionResult.Status.ENVIRONMENT_ERROR) {
                    unavailableReason = "실행 환경 장애로 실행하지 않았습니다.";
                }
            }

            return new ExecutionResult(preparationResult, testResults);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }
}
