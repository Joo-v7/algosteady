package com.algosteady.judging.repository;

import java.util.UUID;

/** 작업별 테스트 통과 여부 요약 */
public interface ExecutionTestSummary {

    /**
     * 작업 ID를 반환한다.
     * @return 요약 대상 작업 ID
     */
    UUID getJobId();

    /**
     * 테스트 ID를 반환한다.
     * @return 요약 대상 테스트 ID
     */
    String getTestId();

    /**
     * 테스트 통과 여부를 반환한다.
     * @return 테스트의 기대 결과 충족 여부
     */
    boolean getPassed();
}
