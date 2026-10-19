package com.algosteady.judging.runner.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 외부 명령 출력 수집 및 실행 제한 검증 */
class ProcessExecutorTests {

    private final ProcessExecutor executor = new ProcessExecutor();

    @Test
    @DisplayName("입력, 표준 출력, 오류 출력과 종료 코드를 수집합니다.")
    void collectsInputBothStreamsAndNonzeroExitCode(@TempDir Path work) throws Exception {
        // given
        Path input = Files.writeString(work.resolve("input"), "입력\n");
        // when
        ProcessExecutor.Result result = executor.execute(List.of("sh", "-c", "cat; printf 'error' >&2; exit 7"),
                input, Duration.ofSeconds(2), 1024, () -> {});

        // then
        assertEquals(ProcessExecutor.Status.COMPLETED, result.status());
        assertEquals("입력\n", result.stdout());
        assertEquals("error", result.stderr());
        assertEquals(7, result.exitCode());
    }

    @Test
    @DisplayName("출력 제한에서 잘린 UTF-8 문자가 출력 크기를 늘리지 않습니다.")
    void limitsCombinedOutputWithoutExpandingTruncatedUtf8() throws Exception {
        // given
        // when
        ProcessExecutor.Result result = executor.execute(List.of("sh", "-c",
                        "i=0; while [ $i -lt 5000 ]; do printf '가나다'; printf '라마바' >&2; i=$((i+1)); done"),
                null, Duration.ofSeconds(3), 1024, () -> {});

        // then
        assertEquals(ProcessExecutor.Status.OUTPUT_LIMIT_EXCEEDED, result.status());
        assertTrue((result.stdout() + result.stderr()).getBytes(StandardCharsets.UTF_8).length <= 1024);
    }

    @Test
    @DisplayName("응답 시간 초과 시 종료 처리를 한 번 호출합니다.")
    void responseTimeoutInvokesTerminationOnce() throws Exception {
        // given
        AtomicInteger calls = new AtomicInteger();
        // when
        ProcessExecutor.Result result = executor.execute(List.of("sh", "-c", "exec sleep 5"), null,
                Duration.ofMillis(200), 1024, calls::incrementAndGet);

        // then
        assertEquals(ProcessExecutor.Status.RESPONSE_TIMEOUT, result.status());
        assertEquals(1, calls.get());
    }
}
