package com.algosteady.learning.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/** 완료한 학습 날짜와 최초 통과 제출의 연결 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LearningHistoryDayResponseDto {

    private LocalDate learningDate;

    private UUID submissionId;
}
