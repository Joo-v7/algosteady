package com.algosteady.exercise.repository;

import com.algosteady.exercise.domain.ExerciseCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 학습 목록 분류 조회 */
public interface ExerciseCategoryRepository extends JpaRepository<ExerciseCategory, String> {

    /**
     * 표시 순서에 따른 분류와 소속 콘텐츠 분류 조회
     * @return 표시 순서 오름차순의 분류 목록
     */
    @EntityGraph(attributePaths = "contentCategories")
    List<ExerciseCategory> findAllByOrderByDisplayOrderAscIdAsc();
}
