package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.util.List;

/** 과제 목록용 최신 버전 요약 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseSummaryResponseDto {

    private String id;

    private String contentVersion;

    private String title;

    private String description;

    private String category;

    private List<String> languages;

    private JsonNode contract;

    private JsonNode criteria;
}
