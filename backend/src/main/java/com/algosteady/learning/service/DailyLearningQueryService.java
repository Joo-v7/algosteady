package com.algosteady.learning.service;

import com.algosteady.learning.domain.DailyLearning;
import com.algosteady.learning.dto.response.DailyLearningResponseDto;
import com.algosteady.learning.repository.DailyLearningCompletionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 오늘의 공통 학습과 로그인 회원의 완료 여부 조회 */
@Service
@RequiredArgsConstructor
public class DailyLearningQueryService {

    private final DailyLearningCommandService dailyLearningCommandService;

    private final DailyLearningCompletionRepository dailyLearningCompletionRepository;

    /**
     * 서버의 오전 6시 기준 날짜로 최초 배정 생성 및 완료 조회
     * @param memberId 인증된 회원 ID, 비로그인은 null
     * @return 오늘의 학습과 현재 회원의 완료 여부
     */
    @Transactional
    public DailyLearningResponseDto findToday(UUID memberId) {
        LocalDate learningDate = DailyLearning.learningDateAt(Instant.now());
        DailyLearning dailyLearning = dailyLearningCommandService.assign(learningDate);
        boolean completed = memberId != null && dailyLearningCompletionRepository.findByMemberIdAndLearningDate(memberId, learningDate).isPresent();

        return DailyLearningResponseDto.builder()
                .learningDate(learningDate)
                .exerciseId(dailyLearning.getExerciseId())
                .completed(completed)
                .build();
    }
}
