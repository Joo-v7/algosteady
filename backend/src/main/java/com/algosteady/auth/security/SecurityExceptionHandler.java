package com.algosteady.auth.security;

import com.algosteady.common.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** 인증·접근 거부를 공통 API 오류 응답으로 변환 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    /**
     * 인증되지 않은 API 요청을 401로 응답
     * @param request 인증이 필요한 요청
     * @param response HTTP 응답
     * @param exception 인증 예외
     * @throws IOException 응답 쓰기 실패
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
    }

    /**
     * 접근 거부·CSRF 검증 실패를 403으로 응답
     * @param request 접근이 거부된 요청
     * @param response HTTP 응답
     * @param exception 접근 거부 예외
     * @throws IOException 응답 쓰기 실패
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, "요청 권한 또는 보안 토큰을 확인해 주세요.");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponseDto.builder()
                .message(message)
                .build());
    }
}
