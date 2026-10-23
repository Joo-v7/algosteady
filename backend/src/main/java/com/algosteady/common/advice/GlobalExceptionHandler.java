package com.algosteady.common.advice;

import com.algosteady.common.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

/** API 공통 오류 응답 처리 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 콘텐츠 및 작업 조회 오류 전달
     * @param exception HTTP 상태와 업무 오류 사유를 가진 예외
     * @return 업무 오류의 HTTP 상태와 오류 응답
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ErrorResponseDto.builder()
                .message(exception.getReason())
                .build());
    }

    /**
     * 필수 필드 검증 결과 전달
     * @param exception 요청 필드 검증에 실패한 예외
     * @return 필드 검증 실패를 설명하는 400 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        return ResponseEntity.badRequest().body(ErrorResponseDto.builder()
                .message(message)
                .build());
    }

    /**
     * JSON 및 URL 형식 오류 전달
     * @param exception 요청 본문 또는 URL 형식 오류
     * @return 입력 형식 오류를 설명하는 400 응답
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponseDto> handleInput(Exception exception) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.builder()
                .message("요청 형식과 필수 입력값을 확인해 주세요.")
                .build());
    }

    /**
     * 내부 오류 원인의 서버 로그 보존
     * @param exception 처리하지 못한 내부 예외
     * @return 내부 오류를 안내하는 500 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
        log.error("API 처리 중 오류가 발생했습니다.", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponseDto.builder()
                .message("서버 처리 중 오류가 발생했습니다.")
                .build());
    }
}
