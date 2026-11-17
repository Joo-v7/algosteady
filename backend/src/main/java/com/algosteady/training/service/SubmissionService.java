package com.algosteady.training.service;

import com.algosteady.judging.dto.response.JobSummaryResponseDto;
import com.algosteady.judging.service.JudgingService;
import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.service.DailyLearningService;
import com.algosteady.member.repository.MemberRepository;
import com.algosteady.training.domain.Submission;
import com.algosteady.training.dto.request.SubmissionRequestDto;
import com.algosteady.training.dto.response.SubmissionDetailsResponseDto;
import com.algosteady.training.dto.response.SubmissionPageResponseDto;
import com.algosteady.training.dto.response.SubmissionResponseDto;
import com.algosteady.training.dto.response.SubmissionSummaryResponseDto;
import com.algosteady.training.repository.SubmissionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 제출 저장 및 제출 기록 조회
 *
 * @author Joo
 * @since 2026.11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final JudgingService judgingService;
    private final DailyLearningService dailyLearningService;
    private final MemberRepository memberRepository;

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

        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));

        // 같은 접수 시각으로 일일 배정·제출·채점 작업을 원자적으로 등록
        Instant acceptedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        dailyLearningService.assign(DailyLearning.learningDateAt(acceptedAt));
        UUID jobId = judgingService.registerGrading(memberId, request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), acceptedAt);
        Submission submission = Submission.create(memberId, request.getExerciseId(), request.getContentVersion(), request.getLanguage(), request.getCode(), request.getReferenceViewed(), jobId, acceptedAt);
        submissionRepository.save(submission);

        return SubmissionResponseDto.from(submission);
    }

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "실습 ID와 지원 언어를 확인해 주세요.");
        }

        if (page < 0 || size < 1 || size > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지는 0 이상, 한 페이지의 건수는 1~20이어야 합니다.");
        }

        // 본인 제출만 과제·언어별 최신순으로 페이지 조회
        Page<Submission> submissionPage = submissionRepository.findByMemberIdAndExerciseIdAndLanguageOrderBySubmittedAtDescIdDesc(memberId, exerciseId, language, PageRequest.of(page, size));
        List<UUID> jobIds = submissionPage.getContent().stream()
                .map(submission -> submission.getJobId())
                .toList();
        // 조회한 제출의 채점 결과를 작업 ID로 일괄 연결
        Map<UUID, JobSummaryResponseDto> summaries = judgingService.findSummariesByIds(jobIds);
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

        return SubmissionDetailsResponseDto.from(submission, judgingService.findById(submission.getJobId(), memberId));
    }
}
