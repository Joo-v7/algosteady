package com.algosteady.training.service;

import com.algosteady.judging.service.JudgingQueryService;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.response.*;
import com.algosteady.training.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 제출 원본 및 채점 결과 조회 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryService {

    private final SubmissionRepository submissionRepository;

    private final JudgingQueryService judgingQueryService;

    /**
     * 과제와 언어별 제출을 최신순으로 조회한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @param page 0부터 시작하는 페이지 번호
     * @param size 한 페이지에 조회할 제출 수
     * @return 제출별 작업 요약과 페이지 정보
     */
    public SubmissionPageResponseDto findAll(String exerciseId, String language, int page, int size) {
        if (exerciseId.isBlank() || !"c".equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과제 ID와 지원 언어를 확인해 주세요.");
        }

        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지는 0 이상, 한 페이지의 건수는 1 이상이어야 합니다.");
        }

        Page<Submission> submissionPage = submissionRepository.findByExerciseIdAndLanguageOrderBySubmittedAtDescIdDesc(exerciseId, language, PageRequest.of(page, size));
        List<UUID> jobIds = submissionPage.getContent().stream()
                .map(submission -> submission.getJobId())
                .toList();
        Map<UUID, JobSummaryResponseDto> summaries = judgingQueryService.findSummariesByIds(jobIds);
        List<SubmissionSummaryResponseDto> content = submissionPage.getContent().stream()
                .map(submission -> SubmissionSummaryResponseDto.from(submission, summaries.get(submission.getJobId())))
                .toList();

        return SubmissionPageResponseDto.builder()
                .content(content)
                .page(submissionPage.getNumber())
                .size(submissionPage.getSize())
                .totalElements(submissionPage.getTotalElements())
                .totalPages(submissionPage.getTotalPages())
                .build();
    }

    /**
     * 제출 원본과 해당 작업의 상세 결과를 조회한다.
     * @param submissionId 조회할 제출 ID
     * @return 제출 원본과 연결된 채점 결과
     */
    public SubmissionDetailsResponseDto findById(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 기록을 찾을 수 없습니다."));

        return SubmissionDetailsResponseDto.from(submission, judgingQueryService.findById(submission.getJobId()));
    }
}
