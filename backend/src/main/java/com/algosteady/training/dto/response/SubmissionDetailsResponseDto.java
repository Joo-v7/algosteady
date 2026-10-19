package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import com.algosteady.judging.dto.response.JobResponseDto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** 제출 원본 및 채점 상세 결과 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionDetailsResponseDto {

    private UUID submissionId;

    private String exerciseId;

    private String contentVersion;

    private String language;

    private String code;

    private boolean referenceViewed;

    private Instant submittedAt;

    private JobResponseDto job;

    /**
     * 저장된 원본을 변경하지 않고 결과와 함께 반환한다.
     * @param submission 저장된 제출 원본
     * @param job 제출에 연결된 작업 상세
     * @return 제출 원본과 채점 상세 응답
     */
    public static SubmissionDetailsResponseDto from(Submission submission, JobResponseDto job) {
        return SubmissionDetailsResponseDto.builder()
                .submissionId(submission.getId())
                .exerciseId(submission.getExerciseId())
                .contentVersion(submission.getContentVersion())
                .language(submission.getLanguage())
                .code(submission.getCode())
                .referenceViewed(submission.isReferenceViewed())
                .submittedAt(submission.getSubmittedAt())
                .job(job)
                .build();
    }
}
