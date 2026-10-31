package com.algosteady.training.service;

import com.algosteady.judging.service.JudgingQueryService;
import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.response.SubmissionDetailsResponseDto;
import com.algosteady.training.dto.response.SubmissionPageResponseDto;
import com.algosteady.training.dto.response.SubmissionSummaryResponseDto;
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
     * 과제와 언어별 제출을 최신순으로 조회
     * @param memberId 인증된 세션의 회원 ID
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @param page 0부터 시작하는 페이지 번호
     * @param size 한 페이지에 조회할 제출 수
     * @return 제출별 작업 요약과 페이지 정보
     */
    public SubmissionPageResponseDto findAll(UUID memberId, String exerciseId, String language, int page, int size) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (exerciseId.isBlank() || !"c".equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과제 ID와 지원 언어를 확인해 주세요.");
        }

        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지는 0 이상, 한 페이지의 건수는 1 이상이어야 합니다.");
        }

        // 본인 제출만 과제·언어별 최신순으로 페이지 조회
        Page<Submission> submissionPage = submissionRepository.findByMemberIdAndExerciseIdAndLanguageOrderBySubmittedAtDescIdDesc(memberId, exerciseId, language, PageRequest.of(page, size));
        List<UUID> jobIds = submissionPage.getContent().stream()
                .map(submission -> submission.getJobId())
                .toList();
        // 조회한 제출의 채점 결과를 작업 ID로 일괄 연결
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
     * 제출 원본과 해당 작업의 상세 결과 조회
     * @param memberId 인증된 세션의 회원 ID
     * @param submissionId 조회할 제출 ID
     * @return 제출 원본과 연결된 채점 결과
     */
    public SubmissionDetailsResponseDto findById(UUID memberId, UUID submissionId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Submission submission = submissionRepository.findByIdAndMemberId(submissionId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 기록을 찾을 수 없습니다."));

        return SubmissionDetailsResponseDto.from(submission, judgingQueryService.findById(submission.getJobId(), memberId));
    }
}
