package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * 제출 메타데이터 및 작업 요약
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionSummaryResponseDto {

    /** 제출 ID */
    private UUID submissionId;
    /** 실습 ID */
    private String exerciseId;
    /** 콘텐츠 버전 */
    private String contentVersion;
    /** 구현 언어 코드 */
    private String language;
    /** 기준 코드 열람 여부 */
    private boolean referenceViewed;
    /** 서버의 제출 접수 시각 */
    private Instant submittedAt;
    /** 제출에 연결된 채점 작업 요약 */
    private JobSummaryResponseDto job;

    /**
     * 제출 메타데이터와 작업 요약의 응답 변환
     * @param submission 저장된 제출 원본
     * @param job 제출에 연결된 작업 요약
     * @return 제출 메타데이터와 작업 요약 응답
     */
    public static SubmissionSummaryResponseDto from(Submission submission, JobSummaryResponseDto job) {
        return SubmissionSummaryResponseDto.builder()
                .submissionId(submission.getId())
                .exerciseId(submission.getExerciseId())
                .contentVersion(submission.getContentVersion())
                .language(submission.getLanguage())
                .referenceViewed(submission.isReferenceViewed())
                .submittedAt(submission.getSubmittedAt())
                .job(job)
                .build();
    }
}
