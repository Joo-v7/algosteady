package com.algosteady.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 로그인한 회원의 기본 정보 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberResponseDto {

    private UUID id;

    private String name;

    private String email;
}
