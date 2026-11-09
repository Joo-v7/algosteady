package com.algosteady.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.algosteady.common.advice.GlobalExceptionHandler;
import com.algosteady.common.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.EOFException;
import java.io.IOException;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 연결 종료의 응답 재작성 방지 및 실제 서버 오류 보존 검증 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @ParameterizedTest
    @MethodSource("disconnectedExceptions")
    @DisplayName("연결 종료와 이를 감싼 응답 쓰기 예외는 오류 응답을 만들지 않습니다.")
    void handleUnexpected_success_withDisconnectedClient(Exception exception) {
        // when
        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleUnexpected(exception);

        // then
        Assertions.assertNull(response);
    }

    @ParameterizedTest
    @MethodSource("serverExceptions")
    @DisplayName("실제 서버·JSON 변환·외부 서버 통신 오류는 기존 500 응답을 유지합니다.")
    void handleUnexpected_success_preservesServerErrors(Exception exception) {
        // when
        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleUnexpected(exception);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("서버 처리 중 오류가 발생했습니다.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("연결 종료는 스택 트레이스 없이 DEBUG 한 줄만 기록합니다.")
    void handleUnexpected_success_logsDisconnectedClientAtDebug() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            // when
            globalExceptionHandler.handleUnexpected(new IOException("Broken pipe"));

            // then
            Assertions.assertEquals(1, appender.list.size());
            Assertions.assertEquals(Level.DEBUG, appender.list.getFirst().getLevel());
            Assertions.assertNull(appender.list.getFirst().getThrowableProxy());
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("일부 JSON 전송 후 연결이 끊기면 확정된 응답에 오류 JSON을 덧붙이지 않습니다.")
    void handleUnexpected_success_doesNotRewriteCommittedResponse() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(globalExceptionHandler)
                .build();

        // when / then
        mockMvc.perform(get("/test/disconnected"))
                .andExpect(content().string("["))
                .andExpect(result -> Assertions.assertTrue(result.getResponse().isCommitted()))
                .andExpect(result -> Assertions.assertInstanceOf(HttpMessageNotWritableException.class, result.getResolvedException()));
    }

    @Test
    @DisplayName("실제 JSON 직렬화 실패는 HTTP 500과 일반 오류 안내를 반환합니다.")
    void handleUnexpected_success_returns500ForSerializationFailure() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(globalExceptionHandler)
                .build();

        // when / then
        mockMvc.perform(get("/test/serialization-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 처리 중 오류가 발생했습니다."));
    }

    private static Stream<Exception> disconnectedExceptions() {
        return Stream.of(
                new IOException("Broken pipe"),
                new IOException("Connection reset by peer"),
                new ClientAbortException(new IOException("Broken pipe")),
                new EOFException(),
                new AsyncRequestNotUsableException("Response not usable after response errors."),
                new HttpMessageNotWritableException("목록 응답 전송 실패", new ClientAbortException(new IOException("Broken pipe")))
        );
    }

    private static Stream<Exception> serverExceptions() {
        return Stream.of(
                new IllegalStateException("내부 처리 실패"),
                new IOException("파일 읽기 실패"),
                new HttpMessageNotWritableException("JSON 변환 실패"),
                new RestClientException("외부 서버 응답 실패", new IOException("Broken pipe"))
        );
    }

    @RestController
    private static class FailureController {

        @GetMapping("/test/disconnected")
        public void disconnected(HttpServletResponse response) throws IOException {
            response.setContentType("application/json");
            response.getWriter().write("[");
            response.flushBuffer();
            throw new HttpMessageNotWritableException("목록 응답 전송 실패", new ClientAbortException(new IOException("Broken pipe")));
        }

        @GetMapping("/test/serialization-error")
        public BrokenPayload serializationError() {
            return new BrokenPayload();
        }
    }

    private static class BrokenPayload {

        public String getValue() {
            throw new IllegalStateException("JSON 직렬화 실패");
        }
    }
}
