package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/**
 * 실행 작업 원본 및 컴파일·테스트·채점 결과
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobResponseDto {

    /** 실행·채점 작업 ID */
    private UUID jobId;
    /** 실습 ID */
    private String exerciseId;
    /** 실행·채점 대상 학습 제목 */
    private String exerciseTitle;
    /** 콘텐츠 버전 */
    private String contentVersion;
    /** 구현 언어 코드 */
    private String language;
    /** 사용자가 작성한 코드 */
    private String code;
    /** 실행할 공개 예제 ID, 전체 채점은 null */
    private String exampleId;
    /** 작업 종류 */
    private String kind;
    /** 작업 진행 상태 */
    private String status;
    /** 서버의 작업 접수 시각 */
    private Instant queuedAt;
    /** 작업 시작 시각 */
    private Instant startedAt;
    /** 작업 종료 시각 */
    private Instant completedAt;
    /** 컴파일·실행 준비 결과, 준비 결과 저장 전은 null */
    private JsonNode preparation;
    /** 결과가 저장된 테스트 목록 */
    private List<TestResultResponseDto> tests;
    /** 전체 채점의 기준별 판정 목록, 공개 예제 실행은 빈 목록 */
    private List<CriterionResultResponseDto> criteria;
    /** 시스템 장애 등 작업 실패 사유 */
    private String message;
}
