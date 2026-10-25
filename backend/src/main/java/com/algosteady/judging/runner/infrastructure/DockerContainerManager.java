package com.algosteady.judging.runner.infrastructure;

import com.algosteady.judging.runner.config.RunnerProperties;
import com.algosteady.judging.runner.model.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

/** 작업 공간 및 Docker 컨테이너 생성 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(RunnerProperties.class)
public class DockerContainerManager {

    private final ProcessExecutor processExecutor;

    private final RunnerProperties runnerProperties;

    /**
     * 제출 코드와 테스트 호출 코드를 준비하고 실행 세션 생성
     * @param executionRequest 언어별 제출 코드와 테스트 입력
     * @return 작업 공간과 컨테이너를 관리하는 실행 세션
     */
    public DockerExecutionSession open(ExecutionRequest executionRequest) throws IOException, InterruptedException {
        String imageName = runnerProperties.images().get(executionRequest.language().getId());

        if (imageName == null || imageName.isBlank()) {
            throw new IllegalArgumentException("실행 이미지가 등록되지 않은 언어입니다: " + executionRequest.language().getId());
        }

        Path workspace = Files.createTempDirectory("algosteady-runner-");
        DockerExecutionSession executionSession = new DockerExecutionSession("algosteady-runner-" + UUID.randomUUID(), workspace, executionRequest.language(), runnerProperties, processExecutor);

        try {
            // 컨테이너에서 읽을 소스 파일과 실행 스크립트 준비
            Files.setPosixFilePermissions(workspace, PosixFilePermissions.fromString("rwxr-xr-x"));
            writeSource(workspace.resolve(executionRequest.language().getSubmissionFilename()), executionRequest.code());
            writeSource(workspace.resolve(executionRequest.language().getHarnessFilename()), executionRequest.harness());
            writeSource(workspace.resolve("input.txt"), "");
            try (InputStream stageScript = new ClassPathResource("runner/stage.sh").getInputStream()) {
                Files.copy(stageScript, workspace.resolve("stage.sh"));
            }
            Files.setPosixFilePermissions(workspace.resolve("stage.sh"), PosixFilePermissions.fromString("rw-r--r--"));

            // 작업용 컨테이너 시작
            executionSession.start(imageName);

            return executionSession;
        } catch (IOException | InterruptedException | RuntimeException | Error exception) {
            try {
                executionSession.close();
            } catch (IOException | InterruptedException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }

            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw exception;
        }
    }

    private void writeSource(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
    }
}
