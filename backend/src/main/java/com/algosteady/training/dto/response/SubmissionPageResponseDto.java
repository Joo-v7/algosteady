package com.algosteady.training.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 제출 목록 및 페이지 정보
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionPageResponseDto {

    /** 현재 페이지의 제출 기록 목록 */
    private List<SubmissionSummaryResponseDto> content;
    /** 0부터 시작하는 현재 페이지 번호 */
    private int page;
    /** 한 페이지의 조회 건수 */
    private int size;
    /** 조회 조건에 맞는 전체 제출 수 */
    private long totalElements;
    /** 전체 페이지 수 */
    private int totalPages;
}
