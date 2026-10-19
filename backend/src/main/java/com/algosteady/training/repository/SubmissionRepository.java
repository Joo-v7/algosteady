package com.algosteady.training.repository;

import com.algosteady.training.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 제출 원본 저장 및 조회 */
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * 과제와 언어별 제출을 최신순 페이지로 조회한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @param pageable 페이지 번호와 정렬을 포함한 조회 조건
     * @return 과제·언어별 최신순 제출 페이지
     */
    Page<Submission> findByExerciseIdAndLanguageOrderBySubmittedAtDescIdDesc(String exerciseId, String language, Pageable pageable);
}

