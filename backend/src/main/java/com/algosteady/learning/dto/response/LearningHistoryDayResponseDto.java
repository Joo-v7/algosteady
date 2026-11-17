package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 완료한 학습 날짜와 최초 통과 제출의 연결
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LearningHistoryDayResponseDto {

    /** 완료한 학습 날짜 */
    private LocalDate learningDate;
    /** 해당 날짜의 최초 통과 작업에 연결된 제출 ID */
    private UUID submissionId;
}
