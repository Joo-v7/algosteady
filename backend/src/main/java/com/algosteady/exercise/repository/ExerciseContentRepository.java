package com.algosteady.exercise.repository;

import com.algosteady.exercise.domain.ExerciseContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 과제별 콘텐츠 버전 조회 */
public interface ExerciseContentRepository extends JpaRepository<ExerciseContent, Long> {

    /**
     * 과제 ID와 콘텐츠 버전으로 원본을 조회한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @return 과제 ID와 버전이 일치하는 콘텐츠
     */
    Optional<ExerciseContent> findByExerciseIdAndContentVersion(String exerciseId, String contentVersion);

    /**
     * 과제의 가장 최근 등록 버전을 조회한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @return 과제의 가장 최근 등록 버전
     */
    Optional<ExerciseContent> findFirstByExerciseIdOrderByIdDesc(String exerciseId);

    /**
     * 콘텐츠를 등록 ID 내림차순으로 조회한다.
     * @return 등록 ID 내림차순의 콘텐츠 목록
     */
    List<ExerciseContent> findAllByOrderByIdDesc();
}
