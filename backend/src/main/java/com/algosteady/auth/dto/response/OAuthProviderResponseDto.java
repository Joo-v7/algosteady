package com.algosteady.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 로그인 가능한 OAuth 제공자 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthProviderResponseDto {

    private String id;

    private String name;

    private String authorizationUrl;
}
