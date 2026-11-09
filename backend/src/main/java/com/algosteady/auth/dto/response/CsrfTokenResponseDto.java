package com.algosteady.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 변경 요청에 사용할 CSRF 토큰과 헤더 이름 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CsrfTokenResponseDto {

    private String headerName;

    private String token;
}
