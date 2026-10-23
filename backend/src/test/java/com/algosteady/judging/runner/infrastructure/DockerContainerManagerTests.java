package com.algosteady.judging.runner.infrastructure;

import com.algosteady.judging.runner.config.RunnerProperties;
import com.algosteady.judging.runner.language.LanguageSpec;
import com.algosteady.judging.runner.model.ExecutionRequest;
import com.algosteady.judging.runner.model.PreparationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 컨테이너 생성 옵션 및 작업 공간 정리 검증 */
class DockerContainerManagerTests {

    private final ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    private final RunnerProperties runnerProperties = new RunnerProperties(Map.of("c", "test-image"),
            Duration.ofSeconds(30), Duration.ofSeconds(2), Duration.ofSeconds(10), 1048576, 1, "512m", 128);
    private final DockerContainerManager dockerContainerManager = new DockerContainerManager(processExecutor, runnerProperties);
    private final ExecutionRequest request = new ExecutionRequest(LanguageSpec.C, "code", "harness", List.of());

    @Test
    @DisplayName("제한된 컨테이너를 생성하고 작업 공간을 정리합니다.")
    void createsRestrictedContainerAndDeletesWorkspace() throws Exception {
        // given
        List<List<String>> commands = new ArrayList<>();
        when(processExecutor.execute(anyList(), any(), any(), anyInt(), any())).thenAnswer(call -> {
            commands.add(call.getArgument(0));
            return new ProcessExecutor.Result(ProcessExecutor.Status.COMPLETED, 0, "", "");
        });

        // when
        try (DockerExecutionSession session = dockerContainerManager.open(request)) {
            List<String> create = commands.getFirst();
            assertEquals(List.of("docker", "create"), create.subList(0, 2));
            assertTrue(create.containsAll(List.of("--network", "none", "--read-only", "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges", "--memory", "512m", "--pids-limit", "128")));
            assertTrue(Files.exists(workspace(create)));
        }

        // then
        assertEquals(List.of("docker", "rm", "-f"), commands.getLast().subList(0, 3));
        assertFalse(Files.exists(workspace(commands.getFirst())));
    }

    @Test
    @DisplayName("시작 실패의 원인을 보존하고 자원을 정리합니다.")
    void openFailureKeepsOriginalErrorAndAttemptsBothCleanupSteps() throws Exception {
        // given
        List<List<String>> commands = new ArrayList<>();
        IOException original = new IOException("start failed");
        IOException cleanup = new IOException("remove failed");
        when(processExecutor.execute(anyList(), any(), any(), anyInt(), any())).thenAnswer(call -> {
            List<String> command = call.getArgument(0);
            commands.add(command);
            if (command.get(1).equals("start")) {
                throw original;
            }
            if (command.get(1).equals("rm")) {
                throw cleanup;
            }
            return new ProcessExecutor.Result(ProcessExecutor.Status.COMPLETED, 0, "", "");
        });

        // when
        IOException thrown = assertThrows(IOException.class, () -> dockerContainerManager.open(request));

        // then
        assertSame(original, thrown);
        assertArrayEquals(new Throwable[]{cleanup}, thrown.getSuppressed());
        assertFalse(Files.exists(workspace(commands.getFirst())));
    }

    @Test
    @DisplayName("Docker 엔진 장애를 컴파일 오류와 구분합니다.")
    void daemonErrorIsNotACompilationFailure() throws Exception {
        // given
        when(processExecutor.execute(anyList(), any(), any(), anyInt(), any())).thenAnswer(call -> {
            List<String> command = call.getArgument(0);
            return new ProcessExecutor.Result(ProcessExecutor.Status.COMPLETED, 1, "",
                    command.get(1).equals("rm") ? "No such container" : "Cannot connect to Docker daemon");
        });

        // when
        try (DockerExecutionSession session = dockerContainerManager.open(request)) {
            assertEquals(PreparationResult.Status.ENVIRONMENT_ERROR, session.prepare().status());
        }
    }

    private Path workspace(List<String> command) {
        String mount = command.get(command.indexOf("--mount") + 1);
        return Path.of(mount.substring("type=bind,source=".length(), mount.indexOf(",target=")));
    }
}
