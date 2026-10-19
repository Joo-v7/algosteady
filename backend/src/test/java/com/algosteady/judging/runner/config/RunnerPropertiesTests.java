package com.algosteady.judging.runner.config;

import com.algosteady.judging.runner.infrastructure.DockerContainerManager;
import com.algosteady.judging.runner.infrastructure.ProcessExecutor;
import com.algosteady.judging.runner.service.CodeExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** 실행 설정 바인딩 및 의존성 연결 검증 */
class RunnerPropertiesTests {
    @Test
    @DisplayName("DB 없이 실행 설정을 바인딩하고 실행부를 연결합니다.")
    void bindsApplicationPropertiesAndWiresRunnerWithoutDatabase() throws Exception {
        // given
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            properties.load(input);
        }
        String[] values = properties.stringPropertyNames().stream()
                .filter(name -> name.startsWith("algosteady.runner."))
                .map(name -> name + "=" + properties.getProperty(name))
                .toArray(String[]::new);

        // when
        new ApplicationContextRunner()
                .withUserConfiguration(ProcessExecutor.class, DockerContainerManager.class, CodeExecutionService.class)
                .withPropertyValues(values)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    RunnerProperties settings = context.getBean(RunnerProperties.class);
                    assertEquals("algosteady-runner-c:14.4.0", settings.images().get("c"));
                    assertEquals(Duration.ofSeconds(30), settings.preparationTimeout());
                    assertEquals(Duration.ofSeconds(2), settings.testTimeout());
                    assertEquals(1048576, settings.outputLimitBytes());
                    assertNotNull(context.getBean(CodeExecutionService.class));
                });
    }
}
