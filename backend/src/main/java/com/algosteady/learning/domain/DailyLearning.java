package com.algosteady.learning.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** 학습 날짜별 공통 학습 배정 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "daily_learning")
public class DailyLearning {

    private static final ZoneId LEARNING_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    private LocalDate learningDate;

    @Column(nullable = false)
    private String exerciseId;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * 한국 시간 오전 6시를 시작으로 학습 날짜 계산
     * @param instant 서버에서 기록한 시각
     * @return 해당 시각의 학습 날짜
     */
    public static LocalDate learningDateAt(Instant instant) {
        return instant.atZone(LEARNING_ZONE).minusHours(6).toLocalDate();
    }

    /**
     * 날짜와 정렬된 과제 ID로 기존 일일 추천 방식 유지
     * @param date 배정할 학습 날짜
     * @param exerciseIds 등록된 과제 ID 목록, 빈 목록 제외
     * @return 선택한 과제 ID
     */
    public static String selectExerciseId(LocalDate date, List<String> exerciseIds) {
        List<String> sorted = exerciseIds.stream().sorted().toList();
        int hash = 0x811c9dc5;
        for (char character : date.toString().toCharArray()) {
            hash = (hash ^ character) * 0x01000193;
        }
        return sorted.get(Integer.remainderUnsigned(hash, sorted.size()));
    }
}
