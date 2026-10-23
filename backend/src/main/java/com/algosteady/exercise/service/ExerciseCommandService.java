package com.algosteady.exercise.service;

import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 콘텐츠 검증 및 버전 등록 */
@Service
@RequiredArgsConstructor
public class ExerciseCommandService implements ApplicationRunner {

    private final ExerciseContentRepository exerciseContentRepository;

    private final JsonMapper jsonMapper;

    private final ExerciseLearningValidator exerciseLearningValidator;

    /**
     * 서버 시작 시 콘텐츠 파일과 테스트 연결 검증
     * @param arguments 애플리케이션 실행 인자
     */
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) throws Exception {
        // 패키징된 과제 메타데이터 파일 조회
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:content/*/exercise.json");

        if (resources.length == 0) {
            throw new IllegalStateException("과제 콘텐츠 파일이 없습니다.");
        }

        for (Resource resource : resources) {
            JsonNode exercise = jsonMapper.readTree(resource.getContentAsString(StandardCharsets.UTF_8));

            // 필수 메타데이터와 학습 내용 검증
            for (String field : new String[]{"id", "version", "title", "description", "category"}) {
                if (!exercise.path(field).isString() || exercise.path(field).asString().isBlank()) {
                    throw new IllegalStateException("필수 콘텐츠 항목이 없습니다: " + field);
                }
            }

            if (!exercise.path("learning").isObject() || !exercise.path("contract").isObject()
                    || !exercise.path("criteria").isArray() || exercise.path("criteria").isEmpty()
                    || !exercise.path("languages").isObject() || exercise.path("languages").isEmpty()) {
                throw new IllegalStateException("학습 내용, 규격, 기준과 언어를 확인해 주세요.");
            }

            exerciseLearningValidator.validate(exercise);

            // 채점 기준 ID의 존재·중복 확인
            Set<String> criterionIds = new HashSet<>();

            for (JsonNode criterion : exercise.path("criteria")) {
                if (criterion.path("id").asString("").isBlank() || criterionIds.contains(criterion.path("id").asString())) {
                    throw new IllegalStateException("채점 기준 ID가 없거나 중복됩니다.");
                }
                criterionIds.add(criterion.path("id").asString());
            }

            ObjectNode sources = jsonMapper.createObjectNode();

            for (Map.Entry<String, JsonNode> language : exercise.path("languages").properties()) {
                if (!language.getKey().equals("c")) {
                    throw new IllegalStateException("지원하지 않는 콘텐츠 언어입니다: " + language.getKey());
                }

                if (!"c17".equals(language.getValue().path("standard").asString(""))) {
                    throw new IllegalStateException("C 콘텐츠 표준은 c17이어야 합니다.");
                }

                ObjectNode source = sources.putObject(language.getKey());

                // 언어별 시작·기준·호출 코드와 테스트 파일 연결
                for (String file : new String[]{"starter", "reference", "harness", "tests"}) {
                    String filename = language.getValue().path(file).asString("");

                    if (filename.isBlank() || filename.startsWith("/") || filename.contains("..")) {
                        throw new IllegalStateException("콘텐츠 파일 경로를 확인해 주세요: " + file);
                    }

                    String text = resource.createRelative(filename).getContentAsString(StandardCharsets.UTF_8);

                    if (text.isBlank()) {
                        throw new IllegalStateException("콘텐츠 파일이 비어 있습니다: " + filename);
                    }

                    if (file.equals("tests")) {
                        source.set(file, jsonMapper.readTree(text));
                    } else {
                        source.put(file, text);
                    }
                }

                if (!source.path("harness").asString().contains("#include \"submission.c\"")) {
                    throw new IllegalStateException("호출 코드의 submission.c 연결을 확인해 주세요.");
                }

                JsonNode tests = source.path("tests");

                if (!tests.isArray() || tests.isEmpty()) {
                    throw new IllegalStateException("테스트 목록이 없습니다.");
                }

                // 테스트 ID·채점 기준 연결·기대값 검증
                Set<String> testIds = new HashSet<>();
                Set<String> testedCriteria = new HashSet<>();
                boolean hasPublicExample = false;

                for (JsonNode test : tests) {
                    if (test.path("id").asString("").isBlank() || testIds.contains(test.path("id").asString())
                            || !criterionIds.contains(test.path("criterionId").asString(""))
                            || !test.path("public").isBoolean() || !test.path("input").isString()
                            || !test.path("description").isString() || !test.path("expected").path("stdout").isString()
                            || !test.path("expected").path("exitCode").isInt()
                            || !test.path("expected").path("stderrRequired").isBoolean()) {
                        throw new IllegalStateException("테스트 ID, 기준 연결, 입력과 기대값을 확인해 주세요.");
                    }
                    testIds.add(test.path("id").asString());
                    testedCriteria.add(test.path("criterionId").asString());
                    hasPublicExample |= test.path("public").asBoolean();
                }

                if (!testedCriteria.equals(criterionIds)) {
                    throw new IllegalStateException("테스트가 연결되지 않은 채점 기준이 있습니다.");
                }

                if (!hasPublicExample) {
                    throw new IllegalStateException("공개 예시가 없습니다.");
                }
            }

            // 검증된 원본을 버전별 스냅샷으로 구성
            ObjectNode snapshot = jsonMapper.createObjectNode();
            snapshot.set("exercise", exercise);
            snapshot.set("sources", sources);
            String exerciseId = exercise.path("id").asString();
            String contentVersion = exercise.path("version").asString();
            Optional<ExerciseContent> existing = exerciseContentRepository.findByExerciseIdAndContentVersion(exerciseId, contentVersion);

            // 동일 버전의 원본 변경 거부, 새 버전만 등록
            if (existing.isPresent()) {
                if (!jsonMapper.readTree(existing.get().getSnapshot()).equals(snapshot)) {
                    throw new IllegalStateException("등록된 콘텐츠와 다릅니다. 콘텐츠 버전을 올려 주세요: " + exerciseId + " " + contentVersion);
                }
            } else {
                exerciseContentRepository.save(ExerciseContent.create(exerciseId, contentVersion, jsonMapper.writeValueAsString(snapshot)));
            }
        }
    }
}
