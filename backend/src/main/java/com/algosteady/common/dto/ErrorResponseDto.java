package com.algosteady.common.dto;

import lombok.*;

/** API 요청 실패 사유 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseDto {

    private String message;
}
