package com.algosteady.training.service;

import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.repository.SubmissionRepository;
import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.service.DailyLearningCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 제출 저장 및 채점 작업 등록 */
@Service
@RequiredArgsConstructor
public class SubmissionCommandService {

    private final SubmissionRepository submissionRepository;

    private final JudgingCommandService judgingCommandService;

    private final DailyLearningCommandService dailyLearningCommandService;

    /**
     * 매 요청마다 새 제출과 채점 작업 생성
     * @param memberId 인증된 세션의 회원 ID
     * @param request 채점할 코드와 기준 코드 열람 정보
     * @return 등록된 제출 ID와 채점 작업 ID
     */
    @Transactional
    public SubmissionResponseDto register(UUID memberId, SubmissionRequestDto request) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 같은 접수 시각으로 일일 배정·제출·채점 작업을 원자적으로 등록
        Instant acceptedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        dailyLearningCommandService.assign(DailyLearning.learningDateAt(acceptedAt));
        UUID jobId = judgingCommandService.registerGrading(memberId, request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), acceptedAt);
        Submission submission = Submission.create(memberId, request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), request.getReferenceViewed(), jobId, acceptedAt);
        submissionRepository.save(submission);

        return SubmissionResponseDto.from(submission);
    }
}
