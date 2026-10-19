package com.algosteady.exercise.controller;

import com.algosteady.exercise.dto.response.*;
import com.algosteady.exercise.service.ExerciseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 과제 콘텐츠 조회 API */
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseQueryController {

    private final ExerciseQueryService exerciseQueryService;

    /**
     * 등록된 과제 목록을 조회한다.
     * @return 과제별 최신 버전 요약 목록
     */
    @GetMapping
    public List<ExerciseSummaryResponseDto> findAll() {
        return exerciseQueryService.findAll();
    }

    /**
     * 최신 콘텐츠의 학습 정보와 공개 예시를 조회한다.
     * @param id 조회할 과제 ID
     * @param language 구현 언어 코드
     * @return 학습 정보, 규격, 시작 코드와 공개 예시
     */
    @GetMapping("/{id}")
    public ExerciseDetailsResponseDto findDetails(@PathVariable String id, @RequestParam String language) {
        return exerciseQueryService.findDetails(id, language);
    }

    /**
     * 선택한 버전의 기준 코드를 조회한다.
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
