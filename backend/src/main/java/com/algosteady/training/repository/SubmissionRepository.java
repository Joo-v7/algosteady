package com.algosteady.training.repository;

import com.algosteady.training.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

/** 제출 원본 저장 및 조회 */
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * 회원이 학습 날짜의 배정 과제에 제출했는지 조회
     * @param memberId 인증된 회원 ID
     * @param exerciseId 해당 날짜의 배정 과제
     * @param start 오전 6시의 학습 시작 시각, 포함
     * @param end 다음 날 오전 6시, 미포함
     * @return 해당 날짜의 제출 존재 여부
     */
    boolean existsByMemberIdAndExerciseIdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(UUID memberId, String exerciseId, Instant start, Instant end);

    /**
     * 학습 완료 작업에 연결된 본인 제출 ID를 코드 본문 없이 일괄 조회
     * @param memberId 로그인한 회원 ID
     * @param jobIds 선택 연도의 완료 작업 ID
     * @return 작업 ID와 제출 ID의 연결
     */
    @Query("""
            SELECT s.jobId AS jobId
                 , s.id AS submissionId
              FROM Submission s
             WHERE s.memberId = :memberId
               AND s.jobId IN :jobIds
            """)
    List<SubmissionLink> findLinksByMemberIdAndJobIds(@Param("memberId") UUID memberId, @Param("jobIds") List<UUID> jobIds);

    /** 학습 기록에서 제출 상세로 이동하기 위한 식별 정보 */
    interface SubmissionLink {
        UUID getJobId();

        UUID getSubmissionId();
    }

    /**
     * 회원·과제·언어별 제출을 최신순 페이지로 조회
     * @param memberId 로그인한 회원 ID
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @param pageable 페이지 번호와 정렬을 포함한 조회 조건
     * @return 과제·언어별 최신순 제출 페이지
     */
    Page<Submission> findByMemberIdAndExerciseIdAndLanguageOrderBySubmittedAtDescIdDesc(UUID memberId, String exerciseId, String language, Pageable pageable);

    /**
     * 회원 본인의 제출 상세 조회
     * @param id 제출 ID
     * @param memberId 로그인한 회원 ID
     * @return 회원 소유의 제출
     */
    Optional<Submission> findByIdAndMemberId(UUID id, UUID memberId);
}

