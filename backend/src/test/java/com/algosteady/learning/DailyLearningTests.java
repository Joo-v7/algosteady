package com.algosteady.learning;

import com.algosteady.learning.domain.DailyLearning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 학습 날짜 경계 및 기존 일일 추천 호환 검증 */
class DailyLearningTests {

    @ParameterizedTest
    @CsvSource({
            "2026-10-25T00:00:00+09:00,2026-10-24",
            "2026-10-25T05:59:59.999+09:00,2026-10-24",
            "2026-10-25T06:00:00+09:00,2026-10-25",
            "2026-10-24T21:00:00Z,2026-10-25",
            "2026-11-01T05:59:59+09:00,2026-10-31",
            "2027-01-01T05:59:59+09:00,2026-12-31",
            "2028-03-01T05:59:59+09:00,2028-02-29"
    })
    @DisplayName("한국 시간 오전 6시·월말·연말·윤년의 학습 날짜를 구분합니다.")
    void learningDateAt_success_withDateBoundaries(String timestamp, String expectedDate) {
        // when
        LocalDate learningDate = DailyLearning.learningDateAt(OffsetDateTime.parse(timestamp).toInstant());

        // then
        Assertions.assertEquals(LocalDate.parse(expectedDate), learningDate);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-24,recursion-factorial", "2026-10-25,queue-circular", "2026-10-29,queue-circular"})
    @DisplayName("기존 프론트 추천과 같은 과제를 선택합니다.")
    void selectExerciseId_success_matchesPreviousRecommendations(String date, String expectedId) {
        // given
        List<String> exerciseIds = List.of("stack-array", "queue-circular", "recursion-factorial");

        // when / then
        Assertions.assertEquals(expectedId, DailyLearning.selectExerciseId(LocalDate.parse(date), exerciseIds));
    }

    @Test
    @DisplayName("목록 순서가 달라도 같은 과제를 선택하고 입력 순서를 유지합니다.")
    void selectExerciseId_success_independentOfListOrder() {
        // given
        List<String> exerciseIds = new ArrayList<>(List.of("stack-array", "queue-circular", "recursion-factorial"));
        List<String> reversed = new ArrayList<>(exerciseIds);
        Collections.reverse(reversed);

        // when / then
        LocalDate date = LocalDate.of(2026, 10, 29);
        Assertions.assertEquals(DailyLearning.selectExerciseId(date, exerciseIds), DailyLearning.selectExerciseId(date, reversed));
        Assertions.assertEquals(List.of("stack-array", "queue-circular", "recursion-factorial"), exerciseIds);
        Assertions.assertEquals("stack-array", DailyLearning.selectExerciseId(date, List.of("stack-array")));
    }
}
