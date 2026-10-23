package com.algosteady.judging.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 공개 예시 실행 작업 ID */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RunResponseDto {

    private UUID jobId;
}
