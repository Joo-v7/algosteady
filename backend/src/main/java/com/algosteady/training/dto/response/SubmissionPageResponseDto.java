package com.algosteady.training.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 제출 목록 및 페이지 정보 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionPageResponseDto {

    private List<SubmissionSummaryResponseDto> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}
