package com.algosteady.learning.repository;

import com.algosteady.learning.domain.DailyLearning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/** 날짜별 학습 배정 조회 및 최초 배정 보존 */
public interface DailyLearningRepository extends JpaRepository<DailyLearning, LocalDate> {

    /**
     * 같은 날짜의 최초 배정만 저장
     * @param learningDate 학습 날짜
     * @param exerciseId 선택한 과제 ID
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO daily_learning (learning_date, exercise_id, created_at)
            VALUES (:learningDate, :exerciseId, CURRENT_TIMESTAMP)
            ON CONFLICT (learning_date) DO NOTHING
            """, nativeQuery = true)
    void assign(@Param("learningDate") LocalDate learningDate, @Param("exerciseId") String exerciseId);
}
