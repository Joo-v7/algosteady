package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 제출 ID 및 채점 작업 ID */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionResponseDto {

    private UUID submissionId;

    private UUID jobId;

    /**
     * 저장된 제출 식별자의 응답 변환
     * @param submission 저장된 제출 원본
     * @return 제출 ID와 작업 ID 응답
     */
    public static SubmissionResponseDto from(Submission submission) {
        return SubmissionResponseDto.builder()
                .submissionId(submission.getId())
                .jobId(submission.getJobId())
                .build();
    }
}
