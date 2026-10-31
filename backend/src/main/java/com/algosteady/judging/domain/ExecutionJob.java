package com.algosteady.judging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** 실행 원본 및 작업 상태 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "execution_job")
public class ExecutionJob {

    public enum Kind {
        RUN,
        GRADING
    }

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @Id
    private UUID id;

    private UUID memberId;

    @Column(nullable = false)
    private String exerciseId;

    @Column(nullable = false)
    private String contentVersion;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "text")
    private String code;

    private String exampleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant queuedAt;

    private Instant startedAt;

    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String preparation;

    @Column(columnDefinition = "text")
    private String message;

    private ExecutionJob(String exerciseId, String contentVersion, String language, String code, String exampleId) {
        this.id = UUID.randomUUID();
        this.exerciseId = exerciseId;
        this.contentVersion = contentVersion;
        this.language = language;
        this.code = code;
        this.exampleId = exampleId;
        this.kind = exampleId == null ? Kind.GRADING : Kind.RUN;
        this.status = Status.QUEUED;
        this.queuedAt = Instant.now();
    }

    /**
     * 검증한 실행 요청을 대기 작업으로 생성
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @param code 사용자가 작성한 코드 원본
     * @param exampleId 실행할 공개 예시 ID
     * @return 새로 생성한 대기 작업
     */
    public static ExecutionJob create(String exerciseId, String contentVersion, String language, String code, String exampleId) {
        return new ExecutionJob(exerciseId, contentVersion, language, code, exampleId);
    }

    /**
     * 전체 테스트를 실행할 채점 작업 생성
     * @param memberId 제출한 회원 ID
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @param code 사용자가 작성한 코드 원본
     * @param acceptedAt 서버에서 제출을 접수한 시각
     * @return 새로 생성한 대기 작업
     */
    public static ExecutionJob createGrading(UUID memberId, String exerciseId, String contentVersion, String language, String code, Instant acceptedAt) {
        ExecutionJob job = new ExecutionJob(exerciseId, contentVersion, language, code, null);
        job.memberId = memberId;
        job.queuedAt = acceptedAt;
        return job;
    }

    /**
     * 작업 실행 시작
     */
    public void markRunning() {
        status = Status.RUNNING;
        startedAt = Instant.now();
    }

    /**
     * 컴파일 결과와 최종 상태 저장
     * @param preparation 직렬화한 컴파일 결과
     * @param environmentError 실행 환경 장애 발생 여부
     */
    public void complete(String preparation, boolean environmentError) {
        this.preparation = preparation;
        status = environmentError ? Status.FAILED : Status.COMPLETED;
        message = environmentError ? "실행 환경 장애로 작업을 완료하지 못했습니다." : null;
        completedAt = Instant.now();
    }

    /**
     * 서버 또는 실행 환경 장애로 작업 종료
     * @param message 작업 실패 사유
     */
    public void markFailed(String message) {
        status = Status.FAILED;
        this.message = message;
        completedAt = Instant.now();
    }
}
