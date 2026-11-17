package com.algosteady.judging.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

/**
 * 실행 코드 및 공개 예시 ID
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter
@NoArgsConstructor
public class RunRequestDto {

    /** 실습 ID */
    @NotBlank(message = "exerciseId 값은 필수입니다.")
    private String exerciseId;
    /** 콘텐츠 버전 */
    @NotBlank(message = "contentVersion 값은 필수입니다.")
    private String contentVersion;
    /** 구현 언어 코드 */
    @NotBlank(message = "language 값은 필수입니다.")
    private String language;
    /** 공개 예제를 실행할 코드 */
    @NotBlank(message = "code 값은 필수입니다.")
    private String code;
    /** 실행할 공개 예제 ID */
    @NotBlank(message = "exampleId 값은 필수입니다.")
    private String exampleId;
}
