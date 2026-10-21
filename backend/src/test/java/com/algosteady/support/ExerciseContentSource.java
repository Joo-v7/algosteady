package com.algosteady.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** 콘텐츠 파일 기반 공통 테스트 대상 조회 */
public final class ExerciseContentSource {

    private ExerciseContentSource() {
    }

    /**
     * 콘텐츠 메타데이터를 확인하고 경로 순서대로 조회한다.
     * @return ID와 버전이 유효한 콘텐츠 리소스 목록
     */
    public static List<Resource> contents() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:content/*/exercise.json");
        Assertions.assertTrue(resources.length > 0, "테스트할 과제 콘텐츠가 없습니다.");
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Set<List<String>> versions = new HashSet<>();

        for (Resource resource : resources) {
            JsonNode content = jsonMapper.readTree(resource.getContentAsString(StandardCharsets.UTF_8));
            for (String field : List.of("id", "version")) {
                Assertions.assertTrue(content.path(field).isString() && !content.path(field).asString().isBlank(), resource.getDescription() + ": " + field);
            }
            Assertions.assertTrue(versions.add(List.of(content.path("id").asString(), content.path("version").asString())), "중복된 콘텐츠 ID·버전: " + resource.getDescription());
        }

        return Arrays.stream(resources)
                .sorted(Comparator.comparing(Resource::getDescription))
                .toList();
    }

    /**
     * API 테스트에 사용할 과제 ID와 버전을 조회한다.
     * @return 콘텐츠 파일에서 읽은 ID·버전 인자 목록
     */
    public static Stream<Arguments> exercises() throws IOException {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        List<Arguments> arguments = new ArrayList<>();
        for (Resource resource : contents()) {
            JsonNode content = jsonMapper.readTree(resource.getContentAsString(StandardCharsets.UTF_8));
            arguments.add(Arguments.of(content.path("id").asString(), content.path("version").asString()));
        }
        return arguments.stream();
    }
}
