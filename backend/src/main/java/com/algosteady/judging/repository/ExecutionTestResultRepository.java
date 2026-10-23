package com.algosteady.judging.repository;

import com.algosteady.judging.domain.ExecutionTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** 작업별 테스트 결과 조회 */
public interface ExecutionTestResultRepository extends JpaRepository<ExecutionTestResult, UUID> {

    /**
     * 작업에 저장된 테스트 결과 조회
     * @param jobId 실행 또는 채점 작업 ID
     * @return 작업에 저장된 테스트 결과 목록
     */
    List<ExecutionTestResult> findAllByJobId(UUID jobId);

    /**
     * 실행 출력 없이 작업별 테스트 통과 여부 조회
     * @param jobIds 요약을 조회할 작업 ID 목록
     * @return 작업별 테스트 ID와 통과 여부 목록
     */
    @Query("select result.jobId as jobId, result.testId as testId, result.passed as passed from ExecutionTestResult result where result.jobId in :jobIds")
    List<ExecutionTestSummary> findSummariesByJobIds(@Param("jobIds") List<UUID> jobIds);
}

