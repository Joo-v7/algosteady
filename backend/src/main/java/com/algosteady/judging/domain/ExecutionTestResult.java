package com.algosteady.judging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** 테스트 실행 결과 및 기대값 비교 결과 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "execution_test_result")
public class ExecutionTestResult {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String testId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String result;

    @Column(nullable = false)
    private boolean passed;

    private ExecutionTestResult(UUID jobId, String testId, String result, boolean passed) {
        this.id = UUID.randomUUID();
        this.jobId = jobId;
        this.testId = testId;
        this.result = result;
        this.passed = passed;
    }

    /**
     * 실행된 테스트 결과를 생성한다.
     * @param jobId 실행 또는 채점 작업 ID
     * @param testId 결과를 저장할 테스트 ID
     * @param result 직렬화한 테스트 실행 결과
     * @param passed 기대 결과 충족 여부
     * @return 새로 생성한 테스트 결과
     */
    public static ExecutionTestResult create(UUID jobId, String testId, String result, boolean passed) {
        return new ExecutionTestResult(jobId, testId, result, passed);
    }
}
