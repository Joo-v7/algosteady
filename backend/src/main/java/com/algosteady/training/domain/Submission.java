package com.algosteady.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 제출 원본 및 채점 작업 ID */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "submission")
public class Submission {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String exerciseId;

    @Column(nullable = false)
    private String contentVersion;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "text")
    private String code;

    @Column(nullable = false)
    private boolean referenceViewed;

    @Column(nullable = false, unique = true)
    private UUID jobId;

    @Column(nullable = false)
    private Instant submittedAt;

    private Submission(String exerciseId, String contentVersion, String language, String code, boolean referenceViewed, UUID jobId) {
        this.id = UUID.randomUUID();
        this.exerciseId = exerciseId;
        this.contentVersion = contentVersion;
        this.language = language;
        this.code = code;
        this.referenceViewed = referenceViewed;
        this.jobId = jobId;
        this.submittedAt = Instant.now();
    }

    /**
     * 원본을 수정하지 않는 새 제출을 생성한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @param code 사용자가 작성한 코드 원본
     * @param referenceViewed 제출 전 기준 코드 열람 여부
     * @param jobId 실행 또는 채점 작업 ID
     * @return 새로 생성한 제출 원본
     */
    public static Submission create(String exerciseId, String contentVersion, String language, String code, boolean referenceViewed, UUID jobId) {
        return new Submission(exerciseId, contentVersion, language, code, referenceViewed, jobId);
    }
}
