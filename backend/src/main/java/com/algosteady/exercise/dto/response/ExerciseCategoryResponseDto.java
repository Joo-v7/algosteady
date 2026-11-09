package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 학습 목록 분류와 소속 콘텐츠 분류 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseCategoryResponseDto {

    private String id;

    private String label;

    private List<String> categories;
}
