package com.algosteady.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 가능한 OAuth 제공자
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthProviderResponseDto {

    /** OAuth 제공자 등록 ID */
    private String id;
    /** 로그인 화면에 표시할 제공자 이름 */
    private String name;
    /** OAuth 로그인 시작 경로 */
    private String authorizationUrl;
}
