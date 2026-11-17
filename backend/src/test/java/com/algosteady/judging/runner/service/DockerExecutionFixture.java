package com.algosteady.judging.runner.service;

import com.algosteady.judging.runner.config.RunnerProperties;
import com.algosteady.judging.runner.infrastructure.DockerContainerManager;
import com.algosteady.judging.runner.infrastructure.ProcessExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Docker 테스트의 공통 실행 환경과 자원 정리 확인
 *
 * @author Joo
 * @since 2026.10
 */
class DockerExecutionFixture {

    final RunnerProperties runnerProperties = new RunnerProperties(Map.of("c", "algosteady-runner-c:14.4.0"),
            Duration.ofSeconds(30), Duration.ofSeconds(2), Duration.ofSeconds(10), 1048576, 1, "512m", 128);
    final ProcessExecutor processExecutor = new ProcessExecutor();
    final CodeExecutionService codeExecutionService = new CodeExecutionService(new DockerContainerManager(processExecutor, runnerProperties));
    private final Set<Path> initialWorkspaces = workspaces();
    private final String containerPrefix = "algosteady-runner-";

    /**
     * 실행용 컨테이너와 임시 작업 공간의 정리 검증
     */
    void assertCleanedUp() throws Exception {
        // given
        ProcessExecutor.Result containers = processExecutor.execute(List.of("docker", "ps", "-a", "--filter", "name=" + containerPrefix,
                        "--format", "{{.Names}}"), null, Duration.ofSeconds(10), 1048576, () -> {});
        assertTrue(containers.succeeded(), containers.diagnostic());
        assertTrue(containers.stdout().isBlank(), containers.stdout());
        assertEquals(initialWorkspaces, workspaces());
    }

    /**
     * 현재 남아 있는 실행용 임시 작업 공간 조회
     * @return 실행용 임시 작업 공간 경로 집합
     */
    private Set<Path> workspaces() {
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("algosteady-runner-"))
                    .collect(Collectors.toSet());
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
