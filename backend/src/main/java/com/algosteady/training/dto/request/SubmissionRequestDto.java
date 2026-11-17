package com.algosteady.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 채점할 코드 및 기준 코드 열람 여부
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionRequestDto {

    /** 실습 ID */
    @NotBlank(message = "exerciseId 값은 필수입니다.")
    private String exerciseId;
    /** 콘텐츠 버전 */
    @NotBlank(message = "contentVersion 값은 필수입니다.")
    private String contentVersion;
    /** 구현 언어 코드 */
    @NotBlank(message = "language 값은 필수입니다.")
    private String language;
    /** 전체 테스트로 채점할 코드 */
    @NotBlank(message = "code 값은 필수입니다.")
    private String code;
    /** 기준 코드 열람 여부 */
    @NotNull(message = "기준 코드 열람 여부는 필수입니다.")
    private Boolean referenceViewed;
}
