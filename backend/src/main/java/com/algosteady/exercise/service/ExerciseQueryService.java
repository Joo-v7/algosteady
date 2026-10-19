package com.algosteady.exercise.service;

import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import com.algosteady.exercise.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

/** 콘텐츠 조회 및 공개 예시 선택 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseQueryService {

    private final ExerciseContentRepository exerciseContentRepository;

    private final JsonMapper jsonMapper;

    /**
     * 과제별로 가장 최근 등록한 버전과 지원 언어를 반환한다.
     * @return 과제별 최신 버전과 지원 언어 목록
     */
    public List<ExerciseSummaryResponseDto> findAll() {
        List<ExerciseSummaryResponseDto> response = new ArrayList<>();
        Set<String> exerciseIds = new HashSet<>();

        for (ExerciseContent content : exerciseContentRepository.findAllByOrderByIdDesc()) {
            if (!exerciseIds.add(content.getExerciseId())) {
                continue;
            }

            JsonNode exercise = jsonMapper.readTree(content.getSnapshot()).path("exercise");

            response.add(ExerciseSummaryResponseDto.builder()
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
     * 기준 코드와 비공개 테스트를 제외한 상세 정보를 반환한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param language 구현 언어 코드
     * @return 학습 정보, 규격, 시작 코드와 공개 예시
     */
    public ExerciseDetailsResponseDto findDetails(String exerciseId, String language) {
        JsonNode snapshot = findSnapshot(exerciseId, null, language);
        JsonNode exercise = snapshot.path("exercise");
        JsonNode source = snapshot.path("sources").path(language);
        ArrayNode examples = jsonMapper.createArrayNode();

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
     * 선택한 버전의 기준 코드를 반환한다.
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
     * 실행부에 전달할 버전별 콘텐츠 원본을 조회한다.
     * @param exerciseId 조회 또는 실행할 과제 ID
     * @param contentVersion 조회 또는 실행할 콘텐츠 버전
     * @param language 구현 언어 코드
     * @return 선택한 버전의 콘텐츠 스냅샷
     */
    public JsonNode findSnapshot(String exerciseId, String contentVersion, String language) {
        if (!"c".equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 언어입니다.");
        }

        ExerciseContent content = (contentVersion == null ? exerciseContentRepository.findFirstByExerciseIdOrderByIdDesc(exerciseId) : exerciseContentRepository.findByExerciseIdAndContentVersion(exerciseId, contentVersion))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "과제 또는 콘텐츠 버전을 찾을 수 없습니다."));
        JsonNode snapshot = jsonMapper.readTree(content.getSnapshot());

        if (!snapshot.path("sources").has(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과제에서 지원하지 않는 언어입니다.");
        }

        return snapshot;
    }

    /**
     * 등록된 공개 예시만 실행 대상으로 선택한다.
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
