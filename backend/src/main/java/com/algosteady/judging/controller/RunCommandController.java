package com.algosteady.judging.controller;

import com.algosteady.judging.dto.request.RunRequestDto;
import com.algosteady.judging.dto.response.RunResponseDto;
import com.algosteady.judging.service.JudgingCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 공개 예시 실행 요청 API */
@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class RunCommandController {

    private final JudgingCommandService judgingCommandService;

    /**
     * 요청을 저장하고 비동기 작업 ID를 반환한다.
     * @param request 실행할 코드와 공개 예시 정보
     * @return 등록된 실행 작업 ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResponseDto register(@Valid @RequestBody RunRequestDto request) {
        return judgingCommandService.register(request);
    }
}
