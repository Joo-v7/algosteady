package com.algosteady.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * API 요청 실패 사유
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseDto {

    /** API 요청 실패 사유 */
    private String message;
}
