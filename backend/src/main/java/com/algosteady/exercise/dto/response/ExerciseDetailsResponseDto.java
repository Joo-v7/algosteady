package com.algosteady.exercise.dto.response;

import lombok.*;
import tools.jackson.databind.JsonNode;

/** 과제 학습 정보·규격·시작 코드·공개 예시 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseDetailsResponseDto {

    private String id;

    private String version;

    private String title;

    private String description;

    private String category;

    private String language;

    private String standard;

    private JsonNode learning;

    private JsonNode contract;

    private JsonNode criteria;

    private String starterCode;

    private JsonNode publicExamples;
}
