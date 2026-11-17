package com.algosteady.exercise.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * 과제 학습 정보·규격·시작 코드·공개 예시
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseDetailsResponseDto {

    /** 실습 ID */
    private String id;
    /** 콘텐츠 버전 */
    private String version;
    /** 학습 제목 */
    private String title;
    /** 학습 개요 */
    private String description;
    /** 콘텐츠 분류 코드 */
    private String category;
    /** 구현 언어 코드 */
    private String language;
    /** 구현 언어 표준 */
    private String standard;
    /** 학습 설명과 시각화 단계 */
    private JsonNode learning;
    /** 구현할 함수와 자료형 등의 실습 규격 */
    private JsonNode contract;
    /** 채점 기준 목록 */
    private JsonNode criteria;
    /** 실습 시작 코드 */
    private String starterCode;
    /** 실행 가능한 입출력 예제 목록 */
    private JsonNode publicExamples;
}
