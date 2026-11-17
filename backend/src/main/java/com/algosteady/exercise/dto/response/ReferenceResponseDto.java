package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 콘텐츠 버전별 기준 코드
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReferenceResponseDto {

    /** 실습 ID */
    private String exerciseId;
    /** 콘텐츠 버전 */
    private String contentVersion;
    /** 구현 언어 코드 */
    private String language;
    /** 선택한 콘텐츠 버전의 기준 코드 */
    private String code;
}
