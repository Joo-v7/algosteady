package com.algosteady.exercise;

import com.algosteady.exercise.domain.ExerciseContent;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import com.algosteady.exercise.service.*;
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

import java.util.Optional;

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

    @Test
    @DisplayName("동일 버전은 재등록하지 않고 변경된 원본은 거부합니다.")
    void register_sameVersionIsImmutable() throws Exception {
        // given
        exerciseCommandService.run(new DefaultApplicationArguments());
        ArgumentCaptor<ExerciseContent> captured = ArgumentCaptor.forClass(ExerciseContent.class);
        Mockito.verify(exerciseContentRepository).save(captured.capture());
        ExerciseContent content = captured.getValue();
        Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion("stack-array", "1.0.0")).thenReturn(Optional.of(content));

        // when
        exerciseCommandService.run(new DefaultApplicationArguments());

        // then
        Mockito.verify(exerciseContentRepository, Mockito.times(1)).save(Mockito.any());

        // given
        ExerciseContent changed = ExerciseContent.create("stack-array", "1.0.0", "{}");
        Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion("stack-array", "1.0.0")).thenReturn(Optional.of(changed));

        // when / then
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> exerciseCommandService.run(new DefaultApplicationArguments()));
        Assertions.assertTrue(failure.getMessage().contains("버전을 올려"));
    }

    @Test
    @DisplayName("상세는 공개 예시만 포함하고 기준 코드와 호출 코드는 별도로 조회합니다.")
    void findDetails_hidesPrivateContent() throws Exception {
        // given
        exerciseCommandService.run(new DefaultApplicationArguments());
        ArgumentCaptor<ExerciseContent> captured = ArgumentCaptor.forClass(ExerciseContent.class);
        Mockito.verify(exerciseContentRepository).save(captured.capture());
        Mockito.when(exerciseContentRepository.findFirstByExerciseIdOrderByIdDesc("stack-array")).thenReturn(Optional.of(captured.getValue()));
        Mockito.when(exerciseContentRepository.findByExerciseIdAndContentVersion("stack-array", "1.0.0")).thenReturn(Optional.of(captured.getValue()));

        // when
        JsonNode details = jsonMapper.valueToTree(exerciseQueryService.findDetails("stack-array", "c"));

        // then
        Assertions.assertFalse(details.has("reference"));
        Assertions.assertFalse(details.has("harness"));
        Assertions.assertFalse(details.has("languages"));
        Assertions.assertEquals(1, details.path("publicExamples").size());

        // when / then
        Assertions.assertTrue(exerciseQueryService.findReference("stack-array", "1.0.0", "c").getCode().contains("s->top = -1"));
        JsonNode snapshot = exerciseQueryService.findSnapshot("stack-array", "1.0.0", "c");
        Assertions.assertThrows(ResponseStatusException.class, () -> exerciseQueryService.findPublicExample(snapshot, "c", "empty-pop"));
        Assertions.assertThrows(ResponseStatusException.class, () -> exerciseQueryService.findSnapshot("stack-array", null, "java"));
    }
}
