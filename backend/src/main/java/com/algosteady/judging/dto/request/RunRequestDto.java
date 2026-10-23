package com.algosteady.judging.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

/** 실행 코드 및 공개 예시 ID */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter
@NoArgsConstructor
public class RunRequestDto {

    @NotBlank(message = "exerciseId 값은 필수입니다.")
    private String exerciseId;

    @NotBlank(message = "contentVersion 값은 필수입니다.")
    private String contentVersion;

    @NotBlank(message = "language 값은 필수입니다.")
    private String language;

    @NotBlank(message = "code 값은 필수입니다.")
    private String code;

    @NotBlank(message = "exampleId 값은 필수입니다.")
    private String exampleId;
}
