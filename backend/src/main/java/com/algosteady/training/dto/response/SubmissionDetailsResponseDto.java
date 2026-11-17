package com.algosteady.training.dto.response;

import com.algosteady.training.domain.Submission;
import com.algosteady.judging.dto.response.JobResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * 제출 원본 및 채점 상세 결과
 *
 * @author Joo
 * @since 2026.10
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionDetailsResponseDto {

    /** 제출 ID */
    private UUID submissionId;
    /** 실습 ID */
    private String exerciseId;
    /** 콘텐츠 버전 */
    private String contentVersion;
    /** 구현 언어 코드 */
    private String language;
    /** 사용자가 작성한 코드 */
    private String code;
    /** 기준 코드 열람 여부 */
    private boolean referenceViewed;
    /** 서버의 제출 접수 시각 */
    private Instant submittedAt;
    /** 제출에 연결된 채점 작업 상세 */
    private JobResponseDto job;

    /**
     * 제출 원본과 채점 결과의 상세 응답 구성
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
