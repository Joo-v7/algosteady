package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 제출 목록용 작업 상태 및 통과 기준 수 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobSummaryResponseDto {

    private UUID jobId;

    private String status;

    private int passedCriteria;

    private int totalCriteria;
}
