package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** 제출 메타데이터 및 작업 요약 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionSummaryResponseDto {

    private UUID submissionId;

    private String exerciseId;

    private String contentVersion;

    private String language;

    private boolean referenceViewed;

    private Instant submittedAt;

    private JobSummaryResponseDto job;

    /**
     * 제출 메타데이터와 조회한 작업 요약을 변환한다.
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
