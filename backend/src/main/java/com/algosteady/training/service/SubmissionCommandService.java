package com.algosteady.training.service;

import com.algosteady.judging.service.JudgingCommandService;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 제출 저장 및 채점 작업 등록 */
@Service
@RequiredArgsConstructor
public class SubmissionCommandService {

    private final SubmissionRepository submissionRepository;

    private final JudgingCommandService judgingCommandService;

    /**
     * 매 요청마다 새 제출과 채점 작업을 생성한다.
     * @param request 채점할 코드와 기준 코드 열람 정보
     * @return 등록된 제출 ID와 채점 작업 ID
     */
    @Transactional
    public SubmissionResponseDto register(SubmissionRequestDto request) {
        UUID jobId = judgingCommandService.registerGrading(request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode());
        Submission submission = Submission.create(request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), request.getReferenceViewed(), jobId);
        submissionRepository.save(submission);

        return SubmissionResponseDto.from(submission);
    }
}
