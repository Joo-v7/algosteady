package com.algosteady.learning.service;

import com.algosteady.exercise.dto.response.ExerciseSummaryResponseDto;
import com.algosteady.exercise.service.ExerciseQueryService;
import com.algosteady.judging.domain.GradingPassedEvent;
import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import com.algosteady.learning.repository.DailyLearningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 학습 날짜별 배정과 회원별 일일 완료 저장 */
@Service
@RequiredArgsConstructor
@Transactional
public class DailyLearningCommandService {

    private final DailyLearningRepository dailyLearningRepository;

    private final DailyLearningCompletionRepository dailyLearningCompletionRepository;

    private final ExerciseQueryService exerciseQueryService;

    /**
     * 날짜별 공통 학습을 최초 한 번 배정
     * @param learningDate 배정할 학습 날짜
     * @return 기존 또는 새로 저장한 배정
     */
    public DailyLearning assign(LocalDate learningDate) {
        Optional<DailyLearning> existing = dailyLearningRepository.findById(learningDate);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<String> exerciseIds = exerciseQueryService.findAll().stream()
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
    public void recordCompletion(GradingPassedEvent event) {
        LocalDate learningDate = DailyLearning.learningDateAt(event.acceptedAt());
        Optional<DailyLearning> dailyLearning = dailyLearningRepository.findById(learningDate);
        if (dailyLearning.isEmpty() || !dailyLearning.get().getExerciseId().equals(event.exerciseId())) {
            return;
        }

        dailyLearningCompletionRepository.complete(event.memberId(), learningDate, event.jobId(), event.completedAt());
    }
}
