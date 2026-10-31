package com.algosteady.learning.repository;

import com.algosteady.learning.domain.DailyLearningCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** 회원별 하루 한 번의 학습 완료 저장 및 조회 */
public interface DailyLearningCompletionRepository extends JpaRepository<DailyLearningCompletion, Long> {

    /**
     * 회원의 학습 날짜별 완료 여부 조회
     * @param memberId 로그인한 회원 ID
     * @param learningDate 학습 날짜
     * @return 저장된 완료 기록
     */
    Optional<DailyLearningCompletion> findByMemberIdAndLearningDate(UUID memberId, LocalDate learningDate);

    /**
     * 회원별 같은 날짜의 최초 통과 기록만 저장
     * @param memberId 통과한 회원 ID
     * @param learningDate 제출 접수 시각의 학습 날짜
     * @param jobId 전체 채점 기준을 통과한 작업 ID
     * @param completedAt 실제 채점 완료 시각
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO daily_learning_completion (member_id, learning_date, job_id, completed_at)
            VALUES (:memberId, :learningDate, :jobId, :completedAt)
            ON CONFLICT (member_id, learning_date) DO NOTHING
            """, nativeQuery = true)
    void complete(@Param("memberId") UUID memberId, @Param("learningDate") LocalDate learningDate, @Param("jobId") UUID jobId, @Param("completedAt") Instant completedAt);
}
