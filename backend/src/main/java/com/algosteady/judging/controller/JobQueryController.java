package com.algosteady.judging.controller;

import com.algosteady.judging.dto.response.JobResponseDto;
import com.algosteady.judging.service.JudgingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 실행 작업 조회 API */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobQueryController {

    private final JudgingQueryService judgingQueryService;

    /**
     * 실행 원본과 컴파일 및 테스트 결과를 조회한다.
     * @param id 조회할 실행 작업 ID
     * @return 작업 원본과 컴파일·테스트 결과
     */
    @GetMapping("/{id}")
    public JobResponseDto findById(@PathVariable UUID id) {
        return judgingQueryService.findById(id);
    }
}
