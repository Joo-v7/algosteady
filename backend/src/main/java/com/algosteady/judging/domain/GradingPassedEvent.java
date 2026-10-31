package com.algosteady.judging.domain;

import java.time.Instant;
import java.util.UUID;

/** 전체 채점 기준을 통과한 작업과 서버 접수·완료 시각 */
public record GradingPassedEvent(UUID jobId, UUID memberId, String exerciseId, Instant acceptedAt, Instant completedAt) {
}
