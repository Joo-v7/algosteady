package com.algosteady.exercise.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** 버전별 콘텐츠 원본 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "exercise_content")
public class ExerciseContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String exerciseId;

    @Column(nullable = false)
    private String contentVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @Column(nullable = false)
    private Instant createdAt;

    private ExerciseContent(String exerciseId, String contentVersion, String snapshot) {
        this.exerciseId = exerciseId;
        this.contentVersion = contentVersion;
        this.snapshot = snapshot;
        this.createdAt = Instant.now();
    }

    /**
     * 검증한 콘텐츠를 새 버전으로 생성한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param snapshot 버전별 과제 정보와 언어별 소스 스냅샷
     * @return 새로 생성한 콘텐츠 버전
     */
    public static ExerciseContent create(String exerciseId, String contentVersion, String snapshot) {
        return new ExerciseContent(exerciseId, contentVersion, snapshot);
    }
}
