package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * 과제 목록용 최신 버전 요약
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseSummaryResponseDto {

    /** 실습 ID */
    private String id;
    /** 콘텐츠 버전 */
    private String contentVersion;
    /** 학습 제목 */
    private String title;
    /** 학습 개요 */
    private String description;
    /** 콘텐츠 분류 코드 */
    private String category;
    /** 지원 언어 코드 목록 */
    private List<String> languages;
    /** 실습 구현 규격 */
    private JsonNode contract;
    /** 채점 기준 목록 */
    private JsonNode criteria;
}
