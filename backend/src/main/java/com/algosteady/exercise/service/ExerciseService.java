package com.algosteady.exercise.service;

import com.algosteady.exercise.domain.ExerciseCategory;
import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.dto.response.ExerciseCategoryResponseDto;
import com.algosteady.exercise.dto.response.ExerciseDetailsResponseDto;
import com.algosteady.exercise.dto.response.ExerciseSummaryResponseDto;
import com.algosteady.exercise.dto.response.ReferenceResponseDto;
import com.algosteady.exercise.repository.ExerciseCategoryRepository;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 콘텐츠 검증·버전 등록 및 조회
 *
 * @author Joo
 * @since 2026.11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService implements ApplicationRunner {

    private final ExerciseContentRepository exerciseContentRepository;
    private final JsonMapper jsonMapper;
    private final ExerciseLearningValidator exerciseLearningValidator;
    private final ExerciseCategoryRepository exerciseCategoryRepository;

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

    /**
     * 표시 순서에 따른 학습 분류와 소속 관계 반환
     * @return 학습 목록에서 사용하는 분류 목록
     */
    public List<ExerciseCategoryResponseDto> findCategories() {
        List<ExerciseCategoryResponseDto> response = new ArrayList<>();

        for (ExerciseCategory category : exerciseCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc()) {
            response.add(ExerciseCategoryResponseDto.builder()
                    .id(category.getId())
                    .label(category.getLabel())
                    .categories(category.getContentCategories().stream().sorted().toList())
                    .build());
        }

        return response;
    }

    /**
     * 과제별로 가장 최근 등록한 버전과 지원 언어 반환
     * @return 과제별 최신 버전과 지원 언어 목록
     */
    public List<ExerciseSummaryResponseDto> findAll() {
        // 과제별 최신 등록 콘텐츠만 목록에 포함
        List<ExerciseSummaryResponseDto> response = new ArrayList<>();
        Set<String> exerciseIds = new HashSet<>();

        for (ExerciseContent content : exerciseContentRepository.findAllByOrderByIdDesc()) {
            if (!exerciseIds.add(content.getExerciseId())) {
                continue;
            }

            JsonNode exercise = jsonMapper.readTree(content.getSnapshot()).path("exercise");

            // 최신 버전을 선택한 뒤 등록 ID 오름차순으로 표시
            response.addFirst(ExerciseSummaryResponseDto.builder()
                    .id(content.getExerciseId())
                    .contentVersion(content.getContentVersion())
                    .title(exercise.path("title").asString())
                    .description(exercise.path("description").asString())
                    .category(exercise.path("category").asString())
                    .languages(new ArrayList<>(exercise.path("languages").propertyNames()))
                    .contract(exercise.path("contract"))
                    .criteria(exercise.path("criteria"))
                    .build());
        }

        return response;
    }

    /**
     * 기준 코드와 비공개 테스트를 제외한 상세 정보 반환
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @return 학습 정보, 규격, 시작 코드와 공개 예시
     */
    public ExerciseDetailsResponseDto findDetails(String exerciseId, String language) {
        JsonNode snapshot = findSnapshot(exerciseId, null, language);
        JsonNode exercise = snapshot.path("exercise");
        JsonNode source = snapshot.path("sources").path(language);
        ArrayNode examples = jsonMapper.createArrayNode();

        // 공개 테스트의 입력·기대값만 상세 응답에 포함
        for (JsonNode test : source.path("tests")) {
            if (test.path("public").asBoolean()) {
                ObjectNode example = jsonMapper.createObjectNode();

                for (String field : new String[]{"id", "description", "input", "expected"}) {
                    example.set(field, test.path(field));
                }
                examples.add(example);
            }
        }

        return ExerciseDetailsResponseDto.builder()
                .id(exerciseId)
                .version(exercise.path("version").asString())
                .title(exercise.path("title").asString())
                .description(exercise.path("description").asString())
                .category(exercise.path("category").asString())
                .language(language)
                .standard(exercise.path("languages").path(language).path("standard").asString())
                .learning(exercise.path("learning"))
                .contract(exercise.path("contract"))
                .criteria(exercise.path("criteria"))
                .starterCode(source.path("starter").asString())
                .publicExamples(examples)
                .build();
    }

    /**
     * 선택한 버전의 기준 코드 반환
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @return 선택한 콘텐츠 버전의 기준 코드
     */
    public ReferenceResponseDto findReference(String exerciseId, String contentVersion, String language) {
        JsonNode snapshot = findSnapshot(exerciseId, contentVersion, language);

        return ReferenceResponseDto.builder()
                .exerciseId(exerciseId)
                .contentVersion(contentVersion)
                .language(language)
                .code(snapshot.path("sources").path(language).path("reference").asString())
                .build();
    }

    /**
     * 실행부에 전달할 버전별 콘텐츠 원본 조회
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @return 선택한 버전의 콘텐츠 스냅샷
     */
    public JsonNode findSnapshot(String exerciseId, String contentVersion, String language) {
        if (!"c".equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 언어입니다.");
        }

        // 버전 미지정 시 최신 콘텐츠, 지정 시 해당 버전 조회
        Optional<ExerciseContent> selectedContent;
        if (contentVersion == null) {
            selectedContent = exerciseContentRepository.findFirstByExerciseIdOrderByIdDesc(exerciseId);
        } else {
            selectedContent = exerciseContentRepository.findByExerciseIdAndContentVersion(exerciseId, contentVersion);
        }
        ExerciseContent content = selectedContent.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "실습 또는 콘텐츠 버전을 찾을 수 없습니다."));
        JsonNode snapshot = jsonMapper.readTree(content.getSnapshot());

        if (!snapshot.path("sources").has(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 실습에서 지원하지 않는 언어입니다.");
        }

        return snapshot;
    }

    /**
     * 등록된 공개 예시만 실행 대상으로 선택
     * @param snapshot 버전별 과제 정보와 언어별 소스 스냅샷
     * @param language 구현 언어 코드
     * @param exampleId 실행할 공개 예시 ID
     * @return ID가 일치하는 공개 테스트 정의
     */
    public JsonNode findPublicExample(JsonNode snapshot, String language, String exampleId) {
        for (JsonNode test : snapshot.path("sources").path(language).path("tests")) {
            if (test.path("public").asBoolean() && test.path("id").asString().equals(exampleId)) {
                return test;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 예시를 찾을 수 없습니다.");
    }
}
