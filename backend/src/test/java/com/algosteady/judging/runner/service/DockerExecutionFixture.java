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

import static org.junit.jupiter.api.Assertions.*;

/** Docker 테스트의 공통 실행 환경과 자원 정리 확인 */
class DockerExecutionFixture {

    final RunnerProperties properties = new RunnerProperties(Map.of("c", "algosteady-runner-c:14.4.0"),
            Duration.ofSeconds(30), Duration.ofSeconds(2), Duration.ofSeconds(10), 1048576, 1, "512m", 128);
    final ProcessExecutor executor = new ProcessExecutor();
    final CodeExecutionService service = new CodeExecutionService(new DockerContainerManager(executor, properties));
    private final Set<Path> initialWorkspaces = workspaces();
    private final String containerPrefix = "algosteady-runner-";

    void assertCleanedUp() throws Exception {
        // given
        ProcessExecutor.Result containers = executor.execute(List.of("docker", "ps", "-a", "--filter", "name=" + containerPrefix,
                        "--format", "{{.Names}}"), null, Duration.ofSeconds(10), 1048576, () -> {});
        assertTrue(containers.succeeded(), containers.diagnostic());
        assertTrue(containers.stdout().isBlank(), containers.stdout());
        assertEquals(initialWorkspaces, workspaces());
    }

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
