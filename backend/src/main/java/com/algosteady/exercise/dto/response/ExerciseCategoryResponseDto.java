package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 학습 목록 분류와 소속 콘텐츠 분류
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseCategoryResponseDto {

    /** 학습 목록 분류 ID */
    private String id;
    /** 화면에 표시할 분류 이름 */
    private String label;
    /** 해당 목록 분류에 속한 콘텐츠 분류 코드 목록 */
    private List<String> categories;
}
