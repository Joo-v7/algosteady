package com.algosteady.exercise.controller;

import com.algosteady.exercise.dto.response.ExerciseDetailsResponseDto;
import com.algosteady.exercise.dto.response.ExerciseCategoryResponseDto;
import com.algosteady.exercise.dto.response.ExerciseSummaryResponseDto;
import com.algosteady.exercise.dto.response.ReferenceResponseDto;
import com.algosteady.exercise.service.ExerciseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 과제 콘텐츠 조회 API */
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseQueryController {

    private final ExerciseQueryService exerciseQueryService;

    /**
     * 등록된 과제 목록 조회
     * @return 과제별 최신 버전 요약 목록
     */
    @GetMapping
    public List<ExerciseSummaryResponseDto> findAll() {
        return exerciseQueryService.findAll();
    }

    /**
     * 표시 순서에 따른 학습 분류 조회
     * @return 분류 이름과 소속 콘텐츠 분류 목록
     */
    @GetMapping("/categories")
    public List<ExerciseCategoryResponseDto> findCategories() {
        return exerciseQueryService.findCategories();
    }

    /**
     * 최신 콘텐츠의 학습 정보와 공개 예시 조회
     * @param id 조회할 과제 ID
     * @param language 구현 언어 코드
     * @return 학습 정보, 규격, 시작 코드와 공개 예시
     */
    @GetMapping("/{id}")
    public ExerciseDetailsResponseDto findDetails(@PathVariable String id, @RequestParam String language) {
        return exerciseQueryService.findDetails(id, language);
    }

    /**
     * 선택한 버전의 기준 코드 조회
     * @param id 조회할 과제 ID
     * @param language 구현 언어 코드
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @return 선택한 콘텐츠 버전의 기준 코드
     */
    @GetMapping("/{id}/reference")
    public ReferenceResponseDto findReference(@PathVariable String id, @RequestParam String language, @RequestParam String contentVersion) {
        return exerciseQueryService.findReference(id, contentVersion, language);
    }
}
