package com.algosteady.exercise.dto.response;

import lombok.*;

/** 콘텐츠 버전별 기준 코드 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReferenceResponseDto {

    private String exerciseId;

    private String contentVersion;

    private String language;

    private String code;
}
