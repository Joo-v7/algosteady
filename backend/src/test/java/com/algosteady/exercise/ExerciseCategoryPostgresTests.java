package com.algosteady.exercise;

import com.algosteady.exercise.domain.ExerciseCategory;
import com.algosteady.exercise.repository.ExerciseCategoryRepository;
import com.algosteady.exercise.repository.ExerciseContentRepository;
import com.algosteady.judging.service.ExecutionWorker;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL 기반 학습 분류·표시 순서 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class ExerciseCategoryPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ExerciseCategoryRepository exerciseCategoryRepository;

    @Autowired
    private ExerciseContentRepository exerciseContentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("분류를 DB 표시 순서대로 반환하고 모든 콘텐츠 분류를 한 번씩 연결합니다.")
    void findCategories_success_withContentMappings() throws Exception {
        // given
        List<ExerciseCategory> categories = exerciseCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc();

        // when
        JsonNode response = jsonMapper.readTree(mockMvc.perform(get("/api/exercises/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // then
        Assertions.assertFalse(categories.isEmpty());
        Assertions.assertEquals(categories.size(), response.size());
        Set<String> contentCategories = new HashSet<>();
        for (int index = 0; index < categories.size(); index++) {
            ExerciseCategory category = categories.get(index);
            Assertions.assertEquals(category.getId(), response.get(index).path("id").asString());
            Assertions.assertEquals(category.getLabel(), response.get(index).path("label").asString());
            for (JsonNode member : response.get(index).path("categories")) {
                Assertions.assertTrue(contentCategories.add(member.asString()), "중복된 콘텐츠 분류: " + member.asString());
            }
        }
        exerciseContentRepository.findAll().forEach(content -> {
            String category = jsonMapper.readTree(content.getSnapshot()).path("exercise").path("category").asString();
            Assertions.assertTrue(contentCategories.contains(category), "연결되지 않은 콘텐츠 분류: " + category);
        });
    }

    @Test
    @DisplayName("DB에서 변경한 분류 이름·순서·소속 관계를 API에 반영합니다.")
    void findCategories_success_afterDatabaseChanges() throws Exception {
        // given
        entityManager.createNativeQuery("UPDATE exercise_category SET display_order = display_order + 100").executeUpdate();
        entityManager.createNativeQuery("UPDATE exercise_category SET label = :label, display_order = 0 WHERE id = 'stack'")
                .setParameter("label", "스택 분류 변경")
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE exercise_category_member SET category_id = 'stack' WHERE content_category = 'dp'").executeUpdate();
        entityManager.clear();

        // when / then
        mockMvc.perform(get("/api/exercises/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("stack"))
                .andExpect(jsonPath("$[0].label").value("스택 분류 변경"))
                .andExpect(jsonPath("$[0].categories[0]").value("dp"))
                .andExpect(jsonPath("$[0].categories[1]").value("stack"));
    }
}
