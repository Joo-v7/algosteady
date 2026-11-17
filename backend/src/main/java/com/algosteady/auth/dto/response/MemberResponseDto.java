package com.algosteady.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 로그인한 회원의 기본 정보
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberResponseDto {

    /** 로그인한 회원 ID */
    private UUID id;
    /** 회원 이름 */
    private String name;
    /** 회원 이메일 */
    private String email;
}
