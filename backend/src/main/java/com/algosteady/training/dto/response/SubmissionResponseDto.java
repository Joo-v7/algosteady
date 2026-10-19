package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import lombok.*;

import java.util.UUID;

/** 제출 ID 및 채점 작업 ID */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionResponseDto {

    private UUID submissionId;

    private UUID jobId;

    /**
     * 저장한 제출 식별자를 변환한다.
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
