package com.algosteady.judging.runner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/** 실행 이미지 및 시간·출력·자원 제한 설정 */
@ConfigurationProperties("algosteady.runner")
public record RunnerProperties(
        Map<String, String> images,
        Duration preparationTimeout,
        Duration testTimeout,
        Duration dockerTimeout,
        int outputLimitBytes,
        double cpus,
        String memory,
        int pidsLimit
) {
    public RunnerProperties {
        images = Map.copyOf(images);

        if (preparationTimeout == null || preparationTimeout.isNegative() || preparationTimeout.isZero()
                || testTimeout == null || testTimeout.isNegative() || testTimeout.isZero()
                || dockerTimeout == null || dockerTimeout.isNegative() || dockerTimeout.isZero()
                || outputLimitBytes <= 0 || !Double.isFinite(cpus) || cpus <= 0
                || memory == null || memory.isBlank() || pidsLimit <= 0) {
            throw new IllegalArgumentException("실행 제한 설정은 양수 값이어야 합니다.");
        }
    }
}
