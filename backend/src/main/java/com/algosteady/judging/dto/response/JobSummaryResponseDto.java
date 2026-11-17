package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 제출 목록용 작업 상태 및 통과 기준 수
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobSummaryResponseDto {

    /** 실행·채점 작업 ID */
    private UUID jobId;
    /** 작업 진행 상태 */
    private String status;
    /** 통과한 채점 기준 수 */
    private int passedCriteria;
    /** 전체 채점 기준 수 */
    private int totalCriteria;
}
