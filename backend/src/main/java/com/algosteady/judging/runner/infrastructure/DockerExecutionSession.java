package com.algosteady.judging.runner.infrastructure;

import com.algosteady.judging.runner.config.RunnerProperties;
import com.algosteady.judging.runner.language.LanguageSpec;
import com.algosteady.judging.runner.model.PreparationResult;
import com.algosteady.judging.runner.model.TestExecutionResult;
import com.algosteady.judging.runner.model.TestInput;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Docker 실행 세션 및 자원 관리 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DockerExecutionSession implements AutoCloseable {

    private final String containerName;

    private final Path workspace;

    private final LanguageSpec language;

    private final RunnerProperties runnerProperties;

    private final ProcessExecutor processExecutor;

    private ProcessExecutor.Result startupResult;

    private boolean creationAttempted;

    private boolean closed;

    void start(String imageName) throws IOException, InterruptedException {
        // 생성 도중 실패한 컨테이너도 정리할 수 있도록 생성 시도 기록
        creationAttempted = true;
        startupResult = executeDockerCommand(List.of("docker", "create", "--name", containerName,
                "--network", "none", "--cpus", Double.toString(runnerProperties.cpus()),
                "--memory", runnerProperties.memory(), "--memory-swap", runnerProperties.memory(),
                "--pids-limit", Integer.toString(runnerProperties.pidsLimit()), "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges", "--read-only",
                "--tmpfs", "/work:rw,exec,nosuid,nodev,size=128m,uid=10001,gid=10001,mode=0700",
                "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=128m,mode=1777",
                "--mount", "type=bind,source=" + workspace.toRealPath() + ",target=/input,readonly",
                imageName, "sleep", "infinity"));

        if (startupResult.succeeded()) {
            startupResult = executeDockerCommand(List.of("docker", "start", containerName));
        }
    }

    /**
     * 선택한 언어의 컴파일 또는 문법 검사 수행
     * @return 컴파일 상태와 출력·종료 정보
     */
    public PreparationResult prepare() throws IOException, InterruptedException {
        if (!startupResult.succeeded()) {
            return new PreparationResult(PreparationResult.Status.ENVIRONMENT_ERROR,
                    startupResult.stdout(), startupResult.stderr(), null, startupResult.diagnostic());
        }
        StageResult stageResult = runCommand(language.getPreparationCommand(), runnerProperties.preparationTimeout());
        PreparationResult.Status preparationStatus = switch (stageResult.status()) {
            case COMPLETED -> stageResult.exitCode() == 0
                    ? PreparationResult.Status.SUCCEEDED : PreparationResult.Status.FAILED;
            case TIME_LIMIT_EXCEEDED -> PreparationResult.Status.TIME_LIMIT_EXCEEDED;
            case OUTPUT_LIMIT_EXCEEDED -> PreparationResult.Status.OUTPUT_LIMIT_EXCEEDED;
            case ENVIRONMENT_ERROR -> PreparationResult.Status.ENVIRONMENT_ERROR;
            case NOT_EXECUTED -> throw new IllegalStateException("준비 단계가 실행되지 않았습니다.");
        };

        return new PreparationResult(preparationStatus, stageResult.stdout(), stageResult.stderr(), stageResult.exitCode(), stageResult.message());
    }

    /**
     * 테스트 입력을 전달하고 새 프로세스의 실제 실행 결과 반환
     * @param test 실행할 테스트 ID와 입력
     * @return 테스트의 출력·종료 코드·제한 상태·실행 시간
     */
    public TestExecutionResult execute(TestInput test) throws IOException, InterruptedException {
        Files.writeString(workspace.resolve("input.txt"), test.input(), StandardCharsets.UTF_8);
        StageResult stageResult = runCommand(language.getExecutionCommand(), runnerProperties.testTimeout());

        return new TestExecutionResult(test.id(), stageResult.status(), stageResult.stdout(), stageResult.stderr(), stageResult.exitCode(),
                stageResult.elapsedNanos(), stageResult.message());
    }

    private StageResult runCommand(List<String> executionCommand, Duration timeout) throws IOException, InterruptedException {
        List<String> dockerArguments = new ArrayList<>(List.of("docker", "exec", "-i", containerName, "sh", "/input/stage.sh",
                Double.toString(timeout.toNanos() / 1_000_000_000.0)));
        dockerArguments.addAll(executionCommand);
        ProcessExecutor.Result commandResult = processExecutor.execute(dockerArguments, workspace.resolve("input.txt"),
                timeout.plus(runnerProperties.dockerTimeout()), runnerProperties.outputLimitBytes(), this::stopCommand);

        if (commandResult.status() == ProcessExecutor.Status.RESPONSE_TIMEOUT) {
            return createEnvironmentError(commandResult, "Docker 실행 응답 시간이 초과되었습니다.");
        }
        // 사용자 프로그램의 종료 정보와 Docker CLI 종료 정보 구분
        ProcessExecutor.Result metadataResult = executeDockerCommand(List.of("docker", "exec", containerName, "cat", "/work/result"));

        if (!metadataResult.succeeded() || commandResult.exitCode() != 0) {
            return createEnvironmentError(commandResult, "실행 결과를 읽지 못했습니다: " + metadataResult.diagnostic());
        }

        String[] metadataFields = metadataResult.stdout().trim().split("\\s+");

        try {
            if (metadataFields.length != 3) {
                throw new IllegalArgumentException("실행 결과 형식 오류");
            }

            int exitCode = Integer.parseInt(metadataFields[0]);
            long elapsedNanos = Long.parseLong(metadataFields[1]);

            if (elapsedNanos < 0 || !(metadataFields[2].equals("0") || metadataFields[2].equals("1"))) {
                throw new IllegalArgumentException("실행 결과 값 오류");
            }

            if (commandResult.status() == ProcessExecutor.Status.OUTPUT_LIMIT_EXCEEDED) {
                return new StageResult(TestExecutionResult.Status.OUTPUT_LIMIT_EXCEEDED, commandResult.stdout(),
                        commandResult.stderr(), exitCode, elapsedNanos, "출력 제한을 초과했습니다.");
            }

            if (metadataFields[2].equals("1")) {
                return new StageResult(TestExecutionResult.Status.TIME_LIMIT_EXCEEDED, commandResult.stdout(),
                        commandResult.stderr(), exitCode, elapsedNanos, "실행 시간 제한을 초과했습니다.");
            }

            return new StageResult(TestExecutionResult.Status.COMPLETED, commandResult.stdout(), commandResult.stderr(),
                    exitCode, elapsedNanos, "");
        } catch (IllegalArgumentException exception) {
            return createEnvironmentError(commandResult, "실행 결과 형식이 올바르지 않습니다.");
        }
    }

    private void stopCommand() throws IOException, InterruptedException {
        ProcessExecutor.Result stopResult = executeDockerCommand(List.of("docker", "exec", containerName, "sh", "-c",
                "pid=$(cat /work/timeout.pid) && /bin/kill -KILL -- -\"$pid\""));

        if (!stopResult.succeeded() && !stopResult.stderr().contains("No such process")) {
            throw new IOException("실행 프로세스 종료 실패: " + stopResult.diagnostic());
        }
    }

    private ProcessExecutor.Result executeDockerCommand(List<String> executionCommand) throws IOException, InterruptedException {
        return processExecutor.execute(executionCommand, null, runnerProperties.dockerTimeout(), runnerProperties.outputLimitBytes(), () -> {});
    }

    private StageResult createEnvironmentError(ProcessExecutor.Result commandResult, String message) {
        return new StageResult(TestExecutionResult.Status.ENVIRONMENT_ERROR, commandResult.stdout(), commandResult.stderr(),
                null, null, message);
    }

    /**
     * 컨테이너와 임시 작업 공간 정리
     */
    @Override
    public void close() throws IOException, InterruptedException {
        if (closed) {
            return;
        }
        closed = true;
        // 자원 정리 중 중단 상태 임시 해제 및 정리 후 복원
        boolean interrupted = Thread.interrupted();
        Exception cleanupFailure = null;

        try {
            if (creationAttempted) {
                ProcessExecutor.Result removeResult = executeDockerCommand(List.of("docker", "rm", "-f", containerName));

                if (!removeResult.succeeded() && !removeResult.stderr().contains("No such container")) {
                    throw new IOException("컨테이너 정리 실패: " + removeResult.diagnostic());
                }
            }
        } catch (IOException | InterruptedException exception) {
            cleanupFailure = exception;
        }
        try (Stream<Path> paths = Files.walk(workspace)) {
            for (Path path : paths
                    .sorted(Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            if (cleanupFailure == null) {
                cleanupFailure = exception;
            } else {
                cleanupFailure.addSuppressed(exception);
            }
        } finally {
            if (interrupted || cleanupFailure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        if (cleanupFailure instanceof InterruptedException exception) {
            throw exception;
        }

        if (cleanupFailure instanceof IOException exception) {
            throw exception;
        }
    }

    private record StageResult(
            TestExecutionResult.Status status,
            String stdout,
            String stderr,
            Integer exitCode,
            Long elapsedNanos,
            String message
    ) {
    }
}
