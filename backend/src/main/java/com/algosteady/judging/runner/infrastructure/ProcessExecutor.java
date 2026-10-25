package com.algosteady.judging.runner.infrastructure;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 외부 명령 실행 및 시간·출력 제한 관리 */
@Component
public class ProcessExecutor {

    /**
     * 외부 명령 실행 및 제한 도달 시 지정된 종료 처리
     * @param command 실행할 명령과 인자 목록
     * @param input 표준 입력으로 사용할 파일 경로, 없으면 null
     * @param timeout 명령 응답 제한 시간
     * @param outputLimitBytes 표준 출력과 오류 출력의 합산 수집 제한
     * @param onLimit 제한 도달 시 실행을 종료하는 처리
     * @return 외부 명령의 상태·종료 코드·표준 출력·오류 출력
     */
    public Result execute(List<String> command, Path input, Duration timeout, int outputLimitBytes, TerminationAction onLimit) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (input != null) {
            processBuilder.redirectInput(input.toFile());
        }
        Process process = processBuilder.start();
        OutputCollector outputCollector = new OutputCollector(outputLimitBytes);
        Thread stdoutThread = Thread.ofVirtual().start(() -> outputCollector.read(process.getInputStream(), outputCollector.stdout));
        Thread stderrThread = Thread.ofVirtual().start(() -> outputCollector.read(process.getErrorStream(), outputCollector.stderr));
        Status status = Status.COMPLETED;
        boolean terminationRequested = false;
        long deadline = System.nanoTime() + timeout.toNanos();

        try {
            if (input == null) {
                process.getOutputStream().close();
            }
            // 시간·출력 제한 도달 시 실행 종료
            while (!process.waitFor(20, TimeUnit.MILLISECONDS)) {
                if (outputCollector.exceeded && !terminationRequested) {
                    onLimit.terminate();
                    terminationRequested = true;
                }

                if (System.nanoTime() >= deadline) {
                    status = Status.RESPONSE_TIMEOUT;

                    if (!terminationRequested) {
                        onLimit.terminate();
                    }
                    process.destroyForcibly();
                    break;
                }
            }

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IOException("호스트 명령이 종료되지 않았습니다.");
            }
            stdoutThread.join(timeout.toMillis());
            stderrThread.join(timeout.toMillis());

            if (stdoutThread.isAlive() || stderrThread.isAlive()) {
                throw new IOException("명령 출력 수집이 종료되지 않았습니다.");
            }

            if (outputCollector.readFailure != null) {
                throw outputCollector.readFailure;
            }

            if (status != Status.RESPONSE_TIMEOUT && outputCollector.exceeded) {
                status = Status.OUTPUT_LIMIT_EXCEEDED;
            }

            return new Result(status, process.exitValue(), outputCollector.text(outputCollector.stdout), outputCollector.text(outputCollector.stderr));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }

    /** 외부 명령 완료 및 제한 상태 */
    public enum Status {
        COMPLETED,
        OUTPUT_LIMIT_EXCEEDED,
        RESPONSE_TIMEOUT
    }

    /** 외부 명령 상태·종료 코드·출력 */
    public record Result(
            Status status,
            int exitCode,
            String stdout,
            String stderr
    ) {
        /**
         * 제한 없이 종료 코드 0으로 완료되었는지 확인
         * @return 제한 없이 종료 코드 0으로 완료했으면 true
         */
        public boolean succeeded() {
            return status == Status.COMPLETED && exitCode == 0;
        }

        /**
         * 명령 실패 원인 반환
         * @return 실패 상태에 따른 진단 메시지
         */
        public String diagnostic() {
            return switch (status) {
                case RESPONSE_TIMEOUT -> "호스트 명령 응답 시간 초과";
                case OUTPUT_LIMIT_EXCEEDED -> "명령 출력 제한 초과";
                case COMPLETED -> stdout + stderr;
            };
        }
    }

    /** 실행 제한 도달 시 종료 처리 */
    @FunctionalInterface
    public interface TerminationAction {
        /**
         * 실행 프로세스 종료
         */
        void terminate() throws IOException, InterruptedException;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class OutputCollector {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        private final int outputLimitBytes;

        private int capturedBytes;

        private volatile boolean exceeded;

        private volatile IOException readFailure;

        private void read(InputStream stream, ByteArrayOutputStream destination) {
            try (stream) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    synchronized (this) {
                        int accepted = Math.min(count, outputLimitBytes - capturedBytes);
                        destination.write(buffer, 0, accepted);
                        capturedBytes += accepted;

                        if (accepted < count) {
                            exceeded = true;
                        }
                    }
                }
            } catch (IOException exception) {
                readFailure = exception;
            }
        }

        private String text(ByteArrayOutputStream stream) throws IOException {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.IGNORE)
                    .decode(ByteBuffer.wrap(stream.toByteArray())).toString();
        }
    }
}
