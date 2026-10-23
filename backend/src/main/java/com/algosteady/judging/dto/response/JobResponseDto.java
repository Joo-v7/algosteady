package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/** 실행 작업 원본 및 컴파일·테스트·채점 결과 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobResponseDto {

    private UUID jobId;

    private String exerciseId;

    private String exerciseTitle;

    private String contentVersion;

    private String language;

    private String code;

    private String exampleId;

    private String kind;

    private String status;

    private Instant queuedAt;

    private Instant startedAt;

    private Instant completedAt;

    private JsonNode preparation;

    private List<TestResultResponseDto> tests;

    private List<CriterionResultResponseDto> criteria;

    private String message;
}
