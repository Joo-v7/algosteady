package com.algosteady.exercise.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

/** 학습 단계와 시각화 데이터 검증 */
@Component
public class ExerciseLearningValidator {

    /**
     * 화면에서 사용하는 학습 데이터의 형식과 연결 확인
     * @param exercise 등록할 콘텐츠 원본
     */
    public void validate(JsonNode exercise) {
        // 공통 개념·연산·참고 내용의 형식 확인
        String exerciseId = exercise.path("id").asString();
        JsonNode learning = exercise.path("learning");
        require(learning.path("concept").path("title").isString(), exerciseId, "개념 제목");
        requireArray(learning.path("concept").path("paragraphs"), "string", exerciseId, "개념 설명");
        require(learning.path("operations").isArray(), exerciseId, "연산 목록");
        for (JsonNode operation : learning.path("operations")) {
            require(operation.path("function").isString() && operation.path("description").isString(), exerciseId, "연산 설명");
        }
        if (learning.has("notes")) {
            require(learning.path("notes").isArray(), exerciseId, "학습 참고 목록");
            for (JsonNode note : learning.path("notes")) {
                require(note.path("title").isString() && note.path("description").isString(), exerciseId, "학습 참고 내용");
            }
        }
        JsonNode steps = learning.path("steps");
        require(steps.isArray() && !steps.isEmpty(), exerciseId, "학습 단계 목록");
        String category = exercise.path("category").asString();
        JsonNode contract = exercise.path("contract");

        // 학습 단계별 시각화 필드와 노드 연결 확인
        for (int index = 0; index < steps.size(); index++) {
            JsonNode step = steps.get(index);
            String location = exerciseId + " " + (index + 1) + "단계";
            require(step.path("operation").isString() && !step.path("operation").asString().isBlank(), location, "연산 이름");
            for (String field : new String[]{"argument", "result"}) {
                JsonNode value = step.path(field);
                require(value.isNull() || value.isString() || value.isNumber(), location, field);
            }
            if (step.has("description")) {
                require(step.path("description").isString(), location, "단계 설명");
            }
            if (step.has("trace") || Set.of("recursion", "hash", "set", "search").contains(category)) {
                requireArray(step.path("trace"), "string", location, "trace");
            }

            switch (category) {
                case "tree", "heap" -> {
                    JsonNode tree = step.path("tree");
                    requireArray(tree, "tree", location, "tree");
                    for (int node = 1; node < tree.size(); node++) {
                        require(tree.get(node).isNull() || !tree.get((node - 1) / 2).isNull(), location, "부모가 없는 트리 노드");
                    }
                    validateNodeIndexes(step, tree.size(), tree, location);
                    if (category.equals("heap")) {
                        requireArray(step.path("values"), "int", location, "values");
                        require(step.path("values").equals(tree), location, "힙 배열과 트리 연결");
                    }
                }
                case "set" -> {
                    JsonNode parents = step.path("parents");
                    requireArray(parents, "int", location, "parents");
                    for (JsonNode parent : parents) {
                        require(parent.asInt() >= -1 && parent.asInt() < parents.size(), location, "부모 인덱스 범위");
                    }
                    // 각 원소에서 대표까지의 부모 연결 순환 확인
                    for (int node = 0; node < parents.size(); node++) {
                        Set<Integer> visited = new HashSet<>();
                        int current = node;
                        while (current != -1) {
                            require(visited.add(current), location, "부모 연결 순환");
                            current = parents.get(current).asInt();
                        }
                    }
                    validateNodeIndexes(step, parents.size(), null, location);
                }
                case "search" -> {
                    JsonNode values = step.path("values");
                    requireArray(values, "int", location, "values");
                    require(step.path("target").isInt() && step.path("low").isInt() && step.path("high").isInt(), location, "탐색 키와 범위");
                    require(step.path("low").asInt() >= 0 && step.path("low").asInt() <= values.size()
                            && step.path("high").asInt() >= -1 && step.path("high").asInt() < values.size(), location, "탐색 범위 인덱스");
                    validateNodeIndexes(step, values.size(), null, location);
                }
                case "hash" -> {
                    JsonNode buckets = step.path("buckets");
                    require(buckets.isArray() && contract.path("capacity").isInt() && contract.path("capacity").asInt() > 0
                            && buckets.size() == contract.path("capacity").asInt(), location, "버킷 수");
                    for (JsonNode bucket : buckets) {
                        requireArray(bucket, "int", location, "버킷 체인");
                    }
                    if (step.has("activeBucket")) {
                        require(step.path("activeBucket").isInt() && step.path("activeBucket").asInt() >= 0
                                && step.path("activeBucket").asInt() < buckets.size(), location, "활성 버킷 인덱스");
                    }
                }
                case "stack", "queue", "deque", "linked-list" -> {
                    JsonNode values = step.path("values");
                    requireArray(values, "int", location, "values");
                    boolean linked = category.equals("linked-list") || contract.path("storage").asString("").equals("linked");
                    if (!linked) {
                        JsonNode capacity = contract.has("initialCapacity") ? step.path("capacity") : contract.path("capacity");
                        require(capacity.isInt() && capacity.asInt() > 0 && values.size() <= capacity.asInt(), location, "배열 용량");
                        if (category.equals("stack")) {
                            require(step.path("top").isInt() && step.path("top").asInt() == values.size() - 1, location, "스택 top");
                        } else {
                            for (String field : new String[]{"front", "rear"}) {
                                require(step.path(field).isInt() && step.path(field).asInt() >= (contract.path("circular").asBoolean() ? 0 : -1)
                                        && step.path(field).asInt() < capacity.asInt(), location, field + " 인덱스");
                            }
                        }
                    }
                }
                case "recursion" -> { }
                default -> throw new IllegalStateException("지원하지 않는 학습 분류입니다: " + category);
            }
        }
    }

    private void validateNodeIndexes(JsonNode step, int size, JsonNode tree, String location) {
        if (step.has("activeNode")) {
            JsonNode active = step.path("activeNode");
            require(active.isInt() && active.asInt() >= 0 && active.asInt() < size, location, "활성 노드 인덱스");
            require(tree == null || !tree.get(active.asInt()).isNull(), location, "활성 노드 연결");
        }
        if (step.has("visited")) {
            requireArray(step.path("visited"), "int", location, "visited");
            for (JsonNode visited : step.path("visited")) {
                require(visited.asInt() >= 0 && visited.asInt() < size, location, "방문 노드 인덱스");
                require(tree == null || !tree.get(visited.asInt()).isNull(), location, "방문 노드 연결");
            }
        }
    }

    private void requireArray(JsonNode values, String type, String location, String field) {
        require(values.isArray(), location, field);
        for (JsonNode value : values) {
            boolean valid = switch (type) {
                case "string" -> value.isString();
                case "tree" -> value.isNull() || value.isInt();
                default -> value.isInt();
            };
            require(valid, location, field + " 원소 형식");
        }
    }

    private void require(boolean valid, String location, String field) {
        if (!valid) {
            throw new IllegalStateException("학습 콘텐츠를 확인해 주세요: " + location + " · " + field);
        }
    }
}
