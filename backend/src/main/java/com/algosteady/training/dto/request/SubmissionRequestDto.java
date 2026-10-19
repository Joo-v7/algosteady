package com.algosteady.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** 채점할 코드 및 기준 코드 열람 여부 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionRequestDto {

    @NotBlank(message = "exerciseId 값은 필수입니다.")
    private String exerciseId;

    @NotBlank(message = "contentVersion 값은 필수입니다.")
    private String contentVersion;

    @NotBlank(message = "language 값은 필수입니다.")
    private String language;

    @NotBlank(message = "code 값은 필수입니다.")
    private String code;

    @NotNull(message = "기준 코드 열람 여부는 필수입니다.")
    private Boolean referenceViewed;
}
