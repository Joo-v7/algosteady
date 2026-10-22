package com.algosteady.exercise;

import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import com.algosteady.exercise.service.*;
import com.algosteady.support.ExerciseContentSource;
import org.springframework.core.io.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 콘텐츠 리소스 및 공개 범위 검증 */
@ExtendWith(MockitoExtension.class)
class ExerciseContentTests {

    @InjectMocks
    private ExerciseCommandService exerciseCommandService;

    @InjectMocks
    private ExerciseQueryService exerciseQueryService;

    @Mock
    private ExerciseContentRepository exerciseContentRepository;

    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().build();

    @Spy
    private ExerciseLearningValidator exerciseLearningValidator = new ExerciseLearningValidator();

    @Test
    @DisplayName("학습 데이터 검증에 실패하면 콘텐츠를 저장하지 않습니다.")
    void register_fail_withInvalidLearning() {
        // given
        Mockito.doThrow(new IllegalStateException("학습 콘텐츠 오류")).when(exerciseLearningValidator).validate(Mockito.any());

        // when / then
        Assertions.assertThrows(IllegalStateException.class, () -> exerciseCommandService.run(new DefaultApplicationArguments()));
        Mockito.verifyNoInteractions(exerciseContentRepository);
    }

    @Test
    @DisplayName("동일 버전은 재등록하지 않고 변경된 원본은 거부합니다.")
    void register_sameVersionIsImmutable() throws Exception {
        // given
        List<Resource> resources = ExerciseContentSource.contents();
        exerciseCommandService.run(new DefaultApplicationArguments());
        ArgumentCaptor<ExerciseContent> captured = ArgumentCaptor.forClass(ExerciseContent.class);
        Mockito.verify(exerciseContentRepository, Mockito.times(resources.size())).save(captured.capture());
        Set<List<String>> expectedVersions = new HashSet<>();
        for (Resource resource : resources) {
            JsonNode metadata = jsonMapper.readTree(resource.getContentAsString(StandardCharsets.UTF_8));
            expectedVersions.add(List.of(metadata.path("id").asString(), metadata.path("version").asString()));
        }
        Assertions.assertEquals(expectedVersions, captured.getAllValues().stream()
                .map(content -> List.of(content.getExerciseId(), content.getContentVersion()))
                .collect(Collectors.toSet()));
        for (ExerciseContent content : captured.getAllValues()) {
            Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion(content.getExerciseId(), content.getContentVersion())).thenReturn(Optional.of(content));
        }

        // when
        exerciseCommandService.run(new DefaultApplicationArguments());

        // then
        Mockito.verify(exerciseContentRepository, Mockito.times(resources.size())).save(Mockito.any());

        // given
        ExerciseContent original = captured.getAllValues().getFirst();
        ExerciseContent changed = ExerciseContent.create(original.getExerciseId(), original.getContentVersion(), "{}");
        Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion(original.getExerciseId(), original.getContentVersion())).thenReturn(Optional.of(changed));

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseCommandService.run(new DefaultApplicationArguments()));
        Assertions.assertTrue(failure.getMessage().contains("버전을 올려"));
    }

    @Test
    @DisplayName("상세는 공개 예시만 포함하고 기준 코드와 호출 코드는 별도로 조회합니다.")
    void findDetails_hidesPrivateContent() throws Exception {
        // given
        List<Resource> resources = ExerciseContentSource.contents();
        exerciseCommandService.run(new DefaultApplicationArguments());
        ArgumentCaptor<ExerciseContent> captured = ArgumentCaptor.forClass(ExerciseContent.class);
        Mockito.verify(exerciseContentRepository, Mockito.times(resources.size())).save(captured.capture());
        for (ExerciseContent content : captured.getAllValues()) {
            String exerciseId = content.getExerciseId();
            String version = content.getContentVersion();
            Mockito.when(exerciseContentRepository.findFirstByExerciseIdOrderByIdDesc(exerciseId)).thenReturn(Optional.of(content));
            Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion(exerciseId, version)).thenReturn(Optional.of(content));

            // when
            JsonNode details = jsonMapper.valueToTree(exerciseQueryService.findDetails(exerciseId, "c"));

            // then
            Assertions.assertFalse(details.has("reference"));
            Assertions.assertFalse(details.has("harness"));
            Assertions.assertFalse(details.has("languages"));
            JsonNode original = jsonMapper.readTree(content.getSnapshot());
            Assertions.assertEquals(original.path("exercise").path("category"), details.path("category"));
            Assertions.assertEquals(original.path("exercise").path("contract"), details.path("contract"));
            Assertions.assertEquals(original.path("exercise").path("learning"), details.path("learning"));
            Assertions.assertEquals(original.path("sources").path("c").path("tests").valueStream()
                    .filter(test -> test.path("public").asBoolean()).count(), details.path("publicExamples").size());
            Assertions.assertEquals(original.path("sources").path("c").path("reference").asString(), exerciseQueryService.findReference(exerciseId, version, "c").getCode());
            if (exerciseId.startsWith("recursion-")) {
                Assertions.assertEquals("recursion", details.path("category").asString());
                Assertions.assertEquals(1, details.path("contract").path("functions").size());
                Assertions.assertTrue(details.path("learning").path("steps").get(0).path("trace").isArray());
            }
            if (exerciseId.equals("queue-circular") || exerciseId.equals("deque-circular")) {
                Assertions.assertTrue(details.path("contract").path("circular").asBoolean());
                Assertions.assertEquals(5, details.path("contract").path("capacity").asInt());
                Assertions.assertEquals(4, details.path("contract").path("usableCapacity").asInt());
                Assertions.assertEquals(exerciseId.equals("deque-circular") ? 9 : 6, details.path("contract").path("functions").size());
            }
            if (exerciseId.equals("list-doubly")) {
                Assertions.assertEquals("linked-list", details.path("category").asString());
                Assertions.assertTrue(details.path("contract").path("circular").asBoolean());
                Assertions.assertTrue(details.path("contract").path("doubly").asBoolean());
                Assertions.assertEquals(4, details.path("contract").path("functions").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
            }
            if (exerciseId.equals("list-circular")) {
                Assertions.assertEquals("linked-list", details.path("category").asString());
                Assertions.assertTrue(details.path("contract").path("circular").asBoolean());
                Assertions.assertEquals(3, details.path("contract").path("functions").size());
            }
            if (exerciseId.equals("list-singly")) {
                Assertions.assertEquals("linked-list", details.path("category").asString());
                Assertions.assertEquals(5, details.path("contract").path("functions").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
            }
            if (exerciseId.equals("tree-inorder")) {
                Assertions.assertEquals("tree", details.path("category").asString());
                Assertions.assertEquals(1, details.path("contract").path("functions").size());
                Assertions.assertEquals(3, details.path("learning").path("steps").get(1).path("activeNode").asInt());
                Assertions.assertEquals(6, details.path("learning").path("steps").get(7).path("visited").size());
                Assertions.assertEquals("[1] [4] [15] [16] [20] [25] ", details.path("learning").path("steps").get(7).path("trace").get(0).asString());
            }
            if (exerciseId.equals("tree-preorder")) {
                Assertions.assertEquals("tree", details.path("category").asString());
                Assertions.assertEquals(1, details.path("contract").path("functions").size());
                Assertions.assertEquals(0, details.path("learning").path("steps").get(1).path("activeNode").asInt());
                Assertions.assertEquals(6, details.path("learning").path("steps").get(7).path("visited").size());
                Assertions.assertTrue(details.path("learning").path("steps").get(1).path("trace").isArray());
            }
            if (exerciseId.equals("tree-binary")) {
                Assertions.assertEquals("tree", details.path("category").asString());
                Assertions.assertEquals(1, details.path("contract").path("functions").size());
                Assertions.assertTrue(details.path("learning").path("steps").get(0).path("tree").isArray());
                Assertions.assertEquals(3, details.path("learning").path("steps").get(3).path("tree").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
            }
            if (exerciseId.equals("deque-linked")) {
                Assertions.assertEquals("deque", details.path("category").asString());
                Assertions.assertEquals("linked", details.path("contract").path("storage").asString());
                Assertions.assertTrue(details.path("contract").path("doubly").asBoolean());
                Assertions.assertEquals(9, details.path("contract").path("functions").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
                Assertions.assertFalse(details.path("contract").has("circular"));
            }
            if (exerciseId.equals("queue-linked")) {
                Assertions.assertEquals("queue", details.path("category").asString());
                Assertions.assertEquals("linked", details.path("contract").path("storage").asString());
                Assertions.assertEquals(6, details.path("contract").path("functions").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
                Assertions.assertFalse(details.path("contract").has("circular"));
            }
            if (exerciseId.equals("stack-linked")) {
                Assertions.assertEquals("stack", details.path("category").asString());
                Assertions.assertEquals("linked", details.path("contract").path("storage").asString());
                Assertions.assertEquals(7, details.path("contract").path("functions").size());
                Assertions.assertFalse(details.path("contract").has("capacity"));
                Assertions.assertFalse(details.path("contract").has("initialCapacity"));
            }
            if (exerciseId.equals("stack-dynamic")) {
                Assertions.assertEquals(1, details.path("contract").path("initialCapacity").asInt());
                Assertions.assertEquals(2, details.path("contract").path("growthFactor").asInt());
                Assertions.assertFalse(details.path("contract").has("capacity"));
            }

            // when / then
            JsonNode snapshot = exerciseQueryService.findSnapshot(exerciseId, version, "c");
            snapshot.path("sources").path("c").path("tests").valueStream()
                    .filter(test -> !test.path("public").asBoolean())
                    .forEach(test -> Assertions.assertThrows(ResponseStatusException.class,
                            () -> exerciseQueryService.findPublicExample(snapshot, "c", test.path("id").asString())));
            Assertions.assertThrows(ResponseStatusException.class, () -> exerciseQueryService.findSnapshot(exerciseId, null, "java"));
        }
    }
}
