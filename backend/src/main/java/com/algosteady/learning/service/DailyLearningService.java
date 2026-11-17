package com.algosteady.learning.service;

import com.algosteady.exercise.dto.response.ExerciseSummaryResponseDto;
import com.algosteady.exercise.service.ExerciseService;
import com.algosteady.judging.domain.GradingPassedEvent;
import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.domain.DailyLearningCompletion;
import com.algosteady.learning.dto.response.DailyLearningResponseDto;
import com.algosteady.learning.dto.response.LearningHistoryDayResponseDto;
import com.algosteady.learning.dto.response.LearningHistoryResponseDto;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import com.algosteady.learning.repository.DailyLearningRepository;
import com.algosteady.training.repository.SubmissionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 오늘의 학습 배정 및 회원별 완료·기록 관리
 *
 * @author Joo
 * @since 2026.11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyLearningService {

    private final DailyLearningRepository dailyLearningRepository;
    private final DailyLearningCompletionRepository dailyLearningCompletionRepository;
    private final ExerciseService exerciseService;
    private final SubmissionRepository submissionRepository;

    /**
     * 날짜별 공통 학습을 최초 한 번 배정
     * @param learningDate 배정할 학습 날짜
     * @return 기존 또는 새로 저장한 배정
     */
    @Transactional
    public DailyLearning assign(LocalDate learningDate) {
        Optional<DailyLearning> existing = dailyLearningRepository.findById(learningDate);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<String> exerciseIds = exerciseService.findAll().stream()
                .map(ExerciseSummaryResponseDto::getId)
                .toList();
        if (exerciseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학습이 없습니다.");
        }

        String exerciseId = DailyLearning.selectExerciseId(learningDate, exerciseIds);
        dailyLearningRepository.assign(learningDate, exerciseId);
        return dailyLearningRepository.findById(learningDate).orElseThrow();
    }

    /**
     * 채점 상태와 같은 트랜잭션에서 접수 날짜의 오늘의 학습 완료 저장
     * @param event 전체 채점 기준을 통과한 회원 작업
     */
    @EventListener
    @Transactional
    public void recordCompletion(GradingPassedEvent event) {
        LocalDate learningDate = DailyLearning.learningDateAt(event.acceptedAt());
        Optional<DailyLearning> dailyLearning = dailyLearningRepository.findById(learningDate);
        if (dailyLearning.isEmpty() || !dailyLearning.get().getExerciseId().equals(event.exerciseId())) {
            return;
        }

        dailyLearningCompletionRepository.complete(event.memberId(), learningDate, event.jobId(), event.completedAt());
    }

    /**
     * 서버 학습 날짜 기준 현황과 선택 연도의 완료 기록 조회
     * @param memberId 인증된 회원 ID
     * @param year 조회 연도, 생략하면 현재 학습 연도
     * @return 누적·연속 완료 일수와 날짜별 최초 통과 제출
     */
    public LearningHistoryResponseDto findHistory(UUID memberId, Integer year) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        LocalDate today = DailyLearning.learningDateAt(Instant.now());
        int selectedYear = year == null ? today.getYear() : year;
        if (selectedYear < 1 || selectedYear > today.getYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회할 학습 연도를 확인해 주세요.");
        }

        List<DailyLearningCompletion> completions = dailyLearningCompletionRepository.findByMemberIdAndLearningDateLessThanEqualOrderByLearningDateDesc(memberId, today);
        boolean completedToday = !completions.isEmpty() && completions.getFirst().getLearningDate().equals(today);

        // 오늘 미완료 시 어제부터 이어진 연속 기록 유지
        LocalDate expectedDate = completedToday ? today : today.minusDays(1);
        int currentStreak = 0;
        for (DailyLearningCompletion completion : completions) {
            if (!completion.getLearningDate().equals(expectedDate)) {
                break;
            }
            currentStreak++;
            expectedDate = expectedDate.minusDays(1);
        }

        List<DailyLearningCompletion> yearlyCompletions = completions.stream()
                .filter(completion -> completion.getLearningDate().getYear() == selectedYear)
                .toList();
        List<UUID> jobIds = yearlyCompletions.stream()
                .map(DailyLearningCompletion::getJobId)
                .toList();
        Map<UUID, UUID> submissionIds = new HashMap<>();
        if (!jobIds.isEmpty()) {
            for (SubmissionRepository.SubmissionLink link : submissionRepository.findLinksByMemberIdAndJobIds(memberId, jobIds)) {
                submissionIds.put(link.getJobId(), link.getSubmissionId());
            }
        }

        // 선택 연도의 완료 날짜와 본인 제출 상세 연결
        List<LearningHistoryDayResponseDto> days = new ArrayList<>();
        for (DailyLearningCompletion completion : yearlyCompletions) {
            days.add(LearningHistoryDayResponseDto.builder()
                    .learningDate(completion.getLearningDate())
                    .submissionId(submissionIds.get(completion.getJobId()))
                    .build());
        }

        return LearningHistoryResponseDto.builder()
                .learningDate(today)
                .completedToday(completedToday)
                .currentStreak(currentStreak)
                .totalCompletedDays(completions.size())
                .year(selectedYear)
                .days(days)
                .build();
    }

    /**
     * 서버의 오전 6시 기준 날짜로 최초 배정 생성 및 완료 조회
     * @param memberId 인증된 회원 ID, 비로그인은 null
     * @return 오늘의 학습과 현재 회원의 완료 여부
     */
    @Transactional
    public DailyLearningResponseDto findToday(UUID memberId) {
        LocalDate learningDate = DailyLearning.learningDateAt(Instant.now());
        DailyLearning dailyLearning = assign(learningDate);
        boolean completed = memberId != null && dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isPresent();
        Instant start = learningDate.atTime(6, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        Instant end = learningDate.plusDays(1).atTime(6, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        boolean attempted = memberId != null && submissionRepository.existsByMemberIdAndExerciseIdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(memberId, dailyLearning.getExerciseId(), start, end);

        return DailyLearningResponseDto.builder()
                .learningDate(learningDate)
                .exerciseId(dailyLearning.getExerciseId())
                .completed(completed)
                .attempted(attempted)
                .build();
    }
}
