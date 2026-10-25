package com.algosteady.exercise.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            if (step.has("trace") || Set.of("recursion", "hash", "set", "search", "sort", "graph", "string", "trie", "exhaustive", "backtracking", "greedy", "dp", "two-pointer", "sliding-window").contains(category)) {
                requireArray(step.path("trace"), "string", location, "trace");
            }

            switch (category) {
                case "dp" -> {
                    switch (contract.path("problem").asString("fibonacci")) {
                        case "fibonacci" -> validateFibonacci(step, contract, location);
                        case "knapsack" -> validateKnapsack(step, contract, location);
                        case "lcs" -> validateLcs(step, contract, location);
                        default -> require(false, location, "동적 계획법 종류");
                    }
                }
                case "greedy" -> {
                    JsonNode activities = step.path("activities");
                    require(activities.isArray() && contract.path("capacity").isInt()
                            && activities.size() <= contract.path("capacity").asInt(), location, "활동 목록·개수");
                    Map<Integer, Integer> positions = new HashMap<>();
                    for (int indexOfActivity = 0; indexOfActivity < activities.size(); indexOfActivity++) {
                        JsonNode activity = activities.get(indexOfActivity);
                        require(activity.path("id").isInt() && activity.path("start").isInt() && activity.path("end").isInt(), location, "활동 ID·시각 형식");
                        int id = activity.path("id").asInt();
                        require(id >= 0 && id < activities.size() && !positions.containsKey(id), location, "활동 ID 범위·중복");
                        require(activity.path("start").asInt() < activity.path("end").asInt(), location, "활동 구간");
                        positions.put(id, indexOfActivity);
                    }
                    require(step.path("processed").isInt() && step.path("processed").asInt() >= 0
                            && step.path("processed").asInt() <= activities.size(), location, "검사한 활동 수");
                    requireArray(step.path("selected"), "int", location, "선택 활동 ID");
                    Set<Integer> selected = new HashSet<>();
                    Integer lastEnd = null;
                    for (JsonNode id : step.path("selected")) {
                        Integer position = positions.get(id.asInt());
                        require(position != null && position < step.path("processed").asInt()
                                && selected.add(id.asInt()), location, "선택 활동 연결·중복");
                        JsonNode activity = activities.get(position);
                        require(lastEnd == null || activity.path("start").asInt() >= lastEnd, location, "선택 활동 겹침");
                        lastEnd = activity.path("end").asInt();
                    }
                    if (lastEnd == null) {
                        require(!step.has("lastEnd"), location, "선택 전 종료 시각");
                    } else {
                        require(step.path("lastEnd").isInt() && step.path("lastEnd").asInt() == lastEnd, location, "마지막 선택 종료 시각");
                    }
                }
                case "backtracking" -> {
                    require(step.path("size").isInt() && contract.path("capacity").isInt(), location, "체스판 크기 형식");
                    int size = step.path("size").asInt();
                    require(size >= 1 && size <= contract.path("capacity").asInt(), location, "체스판 크기");
                    JsonNode queens = step.path("queens");
                    requireArray(queens, "int", location, "행별 퀸 배치");
                    require(queens.size() == size, location, "배치 배열 크기");
                    require(step.path("row").isInt() && step.path("row").asInt() >= 0
                            && step.path("row").asInt() <= size, location, "배치할 행");
                    int row = step.path("row").asInt();
                    require(step.path("solutions").isInt() && step.path("solutions").asInt() >= 0, location, "찾은 해의 개수");
                    for (int placed = 0; placed < size; placed++) {
                        int column = queens.get(placed).asInt();
                        if (placed >= row) {
                            require(column == -1, location, "미배치 행의 초기값");
                            continue;
                        }
                        require(column >= 0 && column < size, location, "퀸의 열 범위");
                        for (int previous = 0; previous < placed; previous++) {
                            int distance = Math.abs(queens.get(previous).asInt() - column);
                            require(distance != 0 && distance != placed - previous, location, "배치한 퀸 간 충돌");
                        }
                    }
                    if (step.has("candidateColumn")) {
                        require(step.path("candidateColumn").isInt() && row < size, location, "검사 후보 위치");
                        int column = step.path("candidateColumn").asInt();
                        require(column >= 0 && column < size && step.path("safe").isBoolean(), location, "검사 후보·안전 여부");
                        boolean safe = true;
                        for (int previous = 0; previous < row; previous++) {
                            int distance = Math.abs(queens.get(previous).asInt() - column);
                            if (distance == 0 || distance == row - previous) {
                                safe = false;
                            }
                        }
                        require(step.path("safe").asBoolean() == safe, location, "후보 충돌 판정");
                    }
                }
                case "exhaustive" -> {
                    JsonNode values = step.path("values");
                    requireArray(values, "int", location, "후보 원소");
                    require(contract.path("capacity").isInt() && values.size() <= contract.path("capacity").asInt(), location, "후보 개수");
                    Set<Integer> candidates = new HashSet<>();
                    for (JsonNode value : values) {
                        require(value.asInt() > 0 && value.asInt() <= values.size() && candidates.add(value.asInt()), location, "후보 범위·중복");
                    }
                    requireArray(step.path("selected"), "int", location, "선택 경로");
                    Set<Integer> selected = new HashSet<>();
                    for (JsonNode value : step.path("selected")) {
                        require(candidates.contains(value.asInt()) && selected.add(value.asInt()), location, "선택 원소·중복");
                    }
                    require(step.path("depth").isInt() && step.path("depth").asInt() >= selected.size()
                            && step.path("depth").asInt() <= values.size(), location, "탐색 깊이");
                    if (step.has("activeValue")) {
                        require(step.path("activeValue").isInt() && candidates.contains(step.path("activeValue").asInt()), location, "현재 후보");
                    }
                }
                case "string" -> {
                    JsonNode text = step.path("text");
                    require(text.isString() && contract.path("capacity").isInt()
                            && text.asString().length() < contract.path("capacity").asInt(), location, "본문 문자열 길이");
                    require(text.asString().chars().allMatch(character -> character >= 32 && character <= 126), location, "본문 ASCII 문자");
                    if (step.has("pattern")) {
                        require(step.path("pattern").isString(), location, "검색 패턴");
                        int length = step.path("pattern").asString().length();
                        require(step.path("offset").isInt() && step.path("offset").asInt() >= 0
                                && step.path("offset").asInt() <= text.asString().length(), location, "패턴 시작 위치");
                        require(step.path("patternIndex").isInt() && step.path("patternIndex").asInt() >= 0
                                && step.path("patternIndex").asInt() <= length, location, "패턴 비교 위치");
                    }
                    if (step.has("tokens")) {
                        requireArray(step.path("tokens"), "string", location, "토큰 배열");
                    }
                    if (step.has("output")) {
                        require(step.path("output").isString(), location, "문자열 결과");
                    }
                }
                case "trie" -> {
                    require(contract.path("capacity").isInt() && contract.path("capacity").asInt() > 0, location, "트라이 단어 용량");
                    int wordCapacity = contract.path("capacity").asInt();
                    require(step.path("word").isString() && step.path("word").asString().matches("[a-z]*")
                            && step.path("word").asString().length() < wordCapacity, location, "트라이 단어");
                    JsonNode nodes = step.path("nodes");
                    require(nodes.isArray() && !nodes.isEmpty(), location, "트라이 노드 목록");
                    Set<String> prefixes = new HashSet<>();
                    for (JsonNode node : nodes) {
                        require(node.path("prefix").isString() && node.path("prefix").asString().matches("[a-z]*")
                                && node.path("prefix").asString().length() < wordCapacity
                                && node.path("terminal").isBoolean(), location, "접두사와 단어 끝 표시");
                        require(prefixes.add(node.path("prefix").asString()), location, "트라이 접두사 중복");
                    }
                    require(prefixes.contains(""), location, "트라이 루트");
                    for (String prefix : prefixes) {
                        require(prefix.isEmpty() || prefixes.contains(prefix.substring(0, prefix.length() - 1)), location, "트라이 부모 연결");
                    }
                    if (step.has("activePrefix")) {
                        require(step.path("activePrefix").isString() && prefixes.contains(step.path("activePrefix").asString()), location, "현재 접두사");
                    }
                }
                case "graph" -> {
                    boolean directed = contract.path("directed").asBoolean();
                    if (contract.has("directed")) {
                        require(contract.path("directed").isBoolean(), location, "그래프 방향 여부");
                    }
                    JsonNode adjacency = step.path("adjacency");
                    require(adjacency.isArray() && contract.path("capacity").isInt()
                            && adjacency.size() <= contract.path("capacity").asInt(), location, "그래프 정점 수");
                    boolean[][] edges = new boolean[adjacency.size()][adjacency.size()];
                    for (int vertex = 0; vertex < adjacency.size(); vertex++) {
                        requireArray(adjacency.get(vertex), "int", location, "인접 정점 목록");
                        for (JsonNode neighbor : adjacency.get(vertex)) {
                            int target = neighbor.asInt();
                            require(target >= 0 && target < adjacency.size() && target != vertex, location, "간선 정점 범위");
                            require(!edges[vertex][target], location, "중복 간선");
                            edges[vertex][target] = true;
                        }
                    }
                    for (int vertex = 0; vertex < adjacency.size(); vertex++) {
                        for (int target = 0; target < adjacency.size(); target++) {
                            require(directed || edges[vertex][target] == edges[target][vertex], location, "무방향 간선 연결");
                        }
                    }
                    requireArray(step.path("visited"), "int", location, "visited");
                    validateNodeIndexes(step, adjacency.size(), null, location);
                    if (contract.path("weighted").asBoolean()) {
                        validateWeightedGraph(step, edges, directed, location);
                    }
                    if (step.has("frontier")) {
                        requireArray(step.path("frontier"), "int", location, "frontier");
                        require(step.path("frontierLabel").isString(), location, "대기 정점 설명");
                        for (JsonNode vertex : step.path("frontier")) {
                            require(vertex.asInt() >= 0 && vertex.asInt() < adjacency.size(), location, "대기 정점 인덱스");
                        }
                    }
                    if (step.has("inDegrees")) {
                        JsonNode inDegrees = step.path("inDegrees");
                        requireArray(inDegrees, "int", location, "진입 차수 배열");
                        require(directed && inDegrees.size() == adjacency.size(), location, "진입 차수 배열 크기·방향");
                        Set<Integer> processed = new HashSet<>();
                        for (JsonNode vertex : step.path("visited")) {
                            require(processed.add(vertex.asInt()), location, "처리 정점 중복");
                        }
                        // 처리한 정점의 간선을 제외한 진입 차수 확인
                        for (int to = 0; to < adjacency.size(); to++) {
                            int remaining = 0;
                            for (int from = 0; from < adjacency.size(); from++) {
                                if (!processed.contains(from) && edges[from][to]) {
                                    remaining++;
                                }
                            }
                            require(inDegrees.get(to).asInt() == remaining, location, "남은 진입 차수");
                        }
                        requireArray(step.path("frontier"), "int", location, "대기 큐");
                        Set<Integer> queued = new HashSet<>();
                        for (JsonNode vertex : step.path("frontier")) {
                            int node = vertex.asInt();
                            require(queued.add(node) && !processed.contains(node) && inDegrees.get(node).asInt() == 0, location, "대기 큐 정점 상태");
                        }
                    }
                }
                case "sort" -> {
                    JsonNode values = step.path("values");
                    requireArray(values, "int", location, "values");
                    require(contract.path("capacity").isInt() && values.size() <= contract.path("capacity").asInt(), location, "정렬 배열 용량");
                    for (String field : new String[]{"active", "sorted"}) {
                        requireArray(step.path(field), "int", location, field);
                        for (JsonNode node : step.path(field)) {
                            require(node.asInt() >= 0 && node.asInt() < values.size(), location, field + " 인덱스");
                        }
                    }
                }
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
                case "two-pointer" -> validateTwoPointer(step, contract, location);
                case "sliding-window" -> validateSlidingWindow(step, contract, location);
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

    /** 고정 구간의 합·누적 최대 합·교체 원소 확인 */
    private void validateSlidingWindow(JsonNode step, JsonNode contract, String location) {
        JsonNode values = step.path("values");
        requireArray(values, "int", location, "슬라이딩 윈도 배열");
        require(contract.path("capacity").isInt() && !values.isEmpty()
                && values.size() <= contract.path("capacity").asInt(), location, "슬라이딩 윈도 배열 크기");
        require(step.path("k").isInt() && step.path("k").asInt() >= 1
                && step.path("k").asInt() <= values.size(), location, "슬라이딩 윈도 길이");
        require(step.path("start").isInt() && step.path("end").isInt(), location, "슬라이딩 윈도 위치");
        int k = step.path("k").asInt();
        int start = step.path("start").asInt();
        int end = step.path("end").asInt();
        require(start >= 0 && end >= start && end < values.size() && end - start + 1 <= k, location, "슬라이딩 윈도 범위");
        long sum = 0;
        for (int index = start; index <= end; index++) {
            sum += values.get(index).asLong();
        }
        require(step.path("sum").isInt() && step.path("sum").asLong() == sum, location, "슬라이딩 윈도 현재 합");

        // 완료한 길이 K 구간들로 최대 합 대조
        long best = Long.MIN_VALUE;
        for (int windowStart = 0; windowStart <= end - k + 1; windowStart++) {
            long windowSum = 0;
            for (int index = windowStart; index < windowStart + k; index++) {
                windowSum += values.get(index).asLong();
            }
            best = Math.max(best, windowSum);
        }
        if (end + 1 < k) {
            require(step.path("best").isNull(), location, "슬라이딩 윈도 초기 부분 합");
        } else {
            require(step.path("best").isInt() && step.path("best").asLong() == best, location, "슬라이딩 윈도 최대 합");
        }
        String phase = step.path("phase").asString();
        switch (phase) {
            case "initial" -> require(start == 0 && end < k, location, "슬라이딩 윈도 초기 구간");
            case "slide" -> {
                require(start > 0 && end - start + 1 == k, location, "슬라이딩 윈도 이동 구간");
                long previousSum = sum - values.get(end).asLong() + values.get(start - 1).asLong();
                require(step.path("previousSum").isInt() && step.path("previousSum").asLong() == previousSum, location, "슬라이딩 윈도 이전 합");
            }
            case "done" -> require(end == values.size() - 1 && end - start + 1 == k, location, "슬라이딩 윈도 완료 구간");
            default -> require(false, location, "슬라이딩 윈도 계산 단계");
        }
        if (!phase.equals("slide")) {
            require(!step.has("previousSum"), location, "슬라이딩 윈도 이동 전 합 표시");
        }
        if (phase.equals("done")) {
            require(step.path("result").isInt() && step.path("result").asLong() == best, location, "슬라이딩 윈도 반환값");
        } else {
            require(step.path("result").isNull(), location, "슬라이딩 윈도 진행 결과");
        }
    }

    /** 정렬 순서·포인터 범위·합 비교 결과 확인 */
    private void validateTwoPointer(JsonNode step, JsonNode contract, String location) {
        JsonNode values = step.path("values");
        requireArray(values, "int", location, "투 포인터 배열");
        require(contract.path("capacity").isInt() && values.size() <= contract.path("capacity").asInt(), location, "투 포인터 배열 크기");
        for (int index = 1; index < values.size(); index++) {
            require(values.get(index - 1).asInt() <= values.get(index).asInt(), location, "투 포인터 정렬 순서");
        }
        require(step.path("target").isInt() && step.path("left").isInt() && step.path("right").isInt(), location, "투 포인터 목표·위치");
        int left = step.path("left").asInt();
        int right = step.path("right").asInt();
        int target = step.path("target").asInt();
        require(left >= 0 && left <= values.size() && right >= -1 && right < values.size()
                && left <= right + 1, location, "투 포인터 인덱스 범위");
        String expectedDecision;
        if (left < right) {
            long sum = values.get(left).asLong() + values.get(right).asLong();
            require(step.path("sum").isInt() && step.path("sum").asLong() == sum, location, "투 포인터 현재 합");
            if (sum == target) {
                expectedDecision = "found";
            } else {
                expectedDecision = sum < target ? "move-left" : "move-right";
            }
        } else {
            require(!step.has("sum"), location, "투 포인터 종료 후 합");
            expectedDecision = "absent";
            for (int i = 0; i < values.size(); i++) {
                for (int j = i + 1; j < values.size(); j++) {
                    require(values.get(i).asLong() + values.get(j).asLong() != target, location, "투 포인터 탐색 실패 판정");
                }
            }
        }
        require(step.path("decision").asString().equals(expectedDecision), location, "투 포인터 이동 판단");
        switch (expectedDecision) {
            case "found" -> require(step.path("result").asString().equals("{" + left + ", " + right + "}"), location, "투 포인터 성공 결과");
            case "absent" -> require(step.path("result").asString().equals("{-1, -1}"), location, "투 포인터 실패 결과");
            default -> require(step.path("result").isNull(), location, "투 포인터 진행 결과");
        }
    }

    /** 피보나치의 저장값·호출 스택 확인 */
    private void validateFibonacci(JsonNode step, JsonNode contract, String location) {
        require(!step.has("problem") || step.path("problem").asString().equals("fibonacci"), location, "피보나치 단계 종류");
        require(step.path("method").isString() && Set.of("memoization", "tabulation").contains(step.path("method").asString()), location, "피보나치 계산 방식");
        require(step.path("target").isInt() && step.path("target").asInt() >= 0
                && contract.path("capacity").isInt() && step.path("target").asInt() < contract.path("capacity").asInt(), location, "목표 항 범위");
        int target = step.path("target").asInt();
        JsonNode values = step.path("values");
        require(values.isArray() && values.size() == target + 1, location, "저장 배열 크기");
        long previous = 0;
        long current = 1;
        for (JsonNode value : values) {
            require(value.isNull() || (value.isInt() && value.asLong() == previous), location, "피보나치 저장값");
            long next = previous + current;
            previous = current;
            current = next;
        }
        if (step.has("activeIndex")) {
            require(step.path("activeIndex").isInt() && step.path("activeIndex").asInt() >= 0
                    && step.path("activeIndex").asInt() <= target, location, "현재 항 인덱스");
        }
        requireArray(step.path("pending"), "int", location, "호출 스택");
        Set<Integer> pending = new HashSet<>();
        for (JsonNode indexOfCall : step.path("pending")) {
            require(indexOfCall.asInt() >= 0 && indexOfCall.asInt() <= target
                    && pending.add(indexOfCall.asInt()), location, "호출 스택 인덱스·중복");
        }
        if (step.path("method").asString().equals("tabulation")) {
            require(pending.isEmpty(), location, "타뷸레이션 호출 스택");
        }
        if (step.has("reused")) {
            require(step.path("reused").isBoolean(), location, "저장값 재사용 표시");
            if (step.path("reused").asBoolean()) {
                require(step.path("method").asString().equals("memoization") && step.has("activeIndex")
                        && !values.get(step.path("activeIndex").asInt()).isNull(), location, "재사용할 저장값");
            }
        }
    }

    /** 배낭 표의 계산 범위·이전 행 참조·선택 비교 확인 */
    private void validateKnapsack(JsonNode step, JsonNode contract, String location) {
        require(step.path("problem").asString().equals("knapsack"), location, "배낭 단계 종류");
        require(contract.path("maxItems").isInt() && contract.path("maxItems").asInt() >= 0
                && contract.path("maxValue").isInt() && contract.path("maxValue").asInt() >= 0
                && contract.path("capacity").isInt() && contract.path("capacity").asInt() > 0, location, "배낭 입력 한도");
        require(step.path("capacity").isInt() && step.path("capacity").asInt() >= 0
                && step.path("capacity").asInt() <= contract.path("capacity").asInt(), location, "배낭 용량");
        int capacity = step.path("capacity").asInt();
        JsonNode items = step.path("items");
        require(items.isArray() && items.size() <= contract.path("maxItems").asInt(), location, "배낭 물건 목록");
        for (JsonNode item : items) {
            require(item.path("weight").isInt() && item.path("weight").asInt() > 0
                    && item.path("weight").asInt() <= contract.path("capacity").asInt(), location, "물건 무게");
            require(item.path("value").isInt() && item.path("value").asInt() >= 0
                    && item.path("value").asInt() <= contract.path("maxValue").asInt(), location, "물건 가치");
        }

        JsonNode table = step.path("table");
        require(table.isArray() && table.size() == items.size() + 1, location, "배낭 표 행 수");
        for (int row = 0; row < table.size(); row++) {
            JsonNode cells = table.get(row);
            require(cells.isArray() && cells.size() == capacity + 1, location, "배낭 표 열 수");
            for (int column = 0; column <= capacity; column++) {
                JsonNode cell = cells.get(column);
                if (cell.isNull()) continue;
                require(cell.isInt() && cell.asInt() >= 0, location, "배낭 표 값");
                long expected = 0;
                if (row > 0) {
                    JsonNode previous = table.get(row - 1);
                    require(previous.get(column).isInt(), location, "배낭 이전 행 계산 순서");
                    expected = previous.get(column).asLong();
                    JsonNode item = items.get(row - 1);
                    int remaining = column - item.path("weight").asInt();
                    if (remaining >= 0) {
                        require(previous.get(remaining).isInt(), location, "배낭 남은 용량 계산 순서");
                        expected = Math.max(expected, previous.get(remaining).asLong() + item.path("value").asLong());
                    }
                }
                require(cell.asLong() == expected, location, "배낭 최적 가치");
            }
        }
        if (step.has("activeCell")) {
            JsonNode active = step.path("activeCell");
            require(active.isArray() && active.size() == 2 && active.get(0).isInt() && active.get(1).isInt()
                    && active.get(0).asInt() >= 0 && active.get(0).asInt() <= items.size()
                    && active.get(1).asInt() >= 0 && active.get(1).asInt() <= capacity, location, "배낭 현재 칸");
        }
        if (step.has("comparison")) {
            require(step.has("activeCell") && step.path("activeCell").get(0).asInt() > 0, location, "배낭 비교할 물건");
            int row = step.path("activeCell").get(0).asInt();
            int column = step.path("activeCell").get(1).asInt();
            JsonNode comparison = step.path("comparison");
            JsonNode previous = table.get(row - 1);
            require(previous.get(column).isInt() && comparison.path("excluded").isInt()
                    && comparison.path("excluded").asInt() == previous.get(column).asInt(), location, "배낭 제외 가치");
            int remaining = column - items.get(row - 1).path("weight").asInt();
            if (remaining < 0) {
                require(comparison.path("included").isNull(), location, "배낭 무게 초과 표시");
            } else {
                require(previous.get(remaining).isInt() && comparison.path("included").isInt()
                        && comparison.path("included").asLong() == previous.get(remaining).asLong() + items.get(row - 1).path("value").asLong(), location, "배낭 포함 가치");
            }
        }
    }

    /** 두 접두사의 LCS 길이와 계산 순서 확인 */
    private void validateLcs(JsonNode step, JsonNode contract, String location) {
        require(step.path("problem").asString().equals("lcs"), location, "LCS 단계 종류");
        require(contract.path("capacity").isInt() && contract.path("capacity").asInt() >= 0, location, "LCS 길이 한도");
        for (String field : new String[]{"a", "b"}) {
            JsonNode text = step.path(field);
            require(text.isString() && text.asString().matches("[A-Z]*")
                    && text.asString().length() <= contract.path("capacity").asInt(), location, "LCS 문자열 " + field);
        }
        String a = step.path("a").asString();
        String b = step.path("b").asString();
        JsonNode table = step.path("table");
        require(table.isArray() && table.size() == a.length() + 1, location, "LCS 표 행 수");
        for (int row = 0; row <= a.length(); row++) {
            JsonNode cells = table.get(row);
            require(cells.isArray() && cells.size() == b.length() + 1, location, "LCS 표 열 수");
            for (int column = 0; column <= b.length(); column++) {
                JsonNode cell = cells.get(column);
                if (cell.isNull()) continue;
                require(cell.isInt() && cell.asInt() >= 0, location, "LCS 표 값");
                int expected = 0;
                if (row > 0 && column > 0) {
                    JsonNode previous = table.get(row - 1);
                    if (a.charAt(row - 1) == b.charAt(column - 1)) {
                        require(previous.get(column - 1).isInt(), location, "LCS 대각선 계산 순서");
                        expected = previous.get(column - 1).asInt() + 1;
                    } else {
                        require(previous.get(column).isInt() && cells.get(column - 1).isInt(), location, "LCS 위·왼쪽 계산 순서");
                        expected = Math.max(previous.get(column).asInt(), cells.get(column - 1).asInt());
                    }
                }
                require(cell.asInt() == expected, location, "LCS 최적 길이");
            }
        }
        if (step.has("activeCell")) {
            JsonNode active = step.path("activeCell");
            require(active.isArray() && active.size() == 2 && active.get(0).isInt() && active.get(1).isInt()
                    && active.get(0).asInt() >= 0 && active.get(0).asInt() <= a.length()
                    && active.get(1).asInt() >= 0 && active.get(1).asInt() <= b.length(), location, "LCS 현재 칸");
        }
    }

    /** 가중치 행렬과 선택 간선·비용의 일치 확인 */
    private void validateWeightedGraph(JsonNode step, boolean[][] edges, boolean directed, String location) {
        int size = edges.length;
        JsonNode weights = step.path("weights");
        require(weights.isArray() && weights.size() == size, location, "가중치 행렬 크기");
        for (JsonNode row : weights) {
            require(row.isArray() && row.size() == size, location, "가중치 행렬 행 크기");
            for (JsonNode weight : row) {
                require(weight.isNull() || weight.isInt(), location, "가중치 형식");
            }
        }
        for (int from = 0; from < size; from++) {
            for (int to = 0; to < size; to++) {
                JsonNode weight = weights.get(from).get(to);
                require(directed || weight.equals(weights.get(to).get(from)), location, "무방향 가중치 일치");
                if (from == to) {
                    require(weight.isInt() && weight.asInt() == 0, location, "대각선 가중치");
                } else {
                    require(edges[from][to] == !weight.isNull(), location, "간선과 가중치 연결");
                }
            }
        }

        Set<String> selected = new HashSet<>();
        long total = 0;
        require(step.has("selectedEdges") || step.has("distances") || step.has("distanceMatrix"), location, "선택 간선 또는 최단 거리");
        if (step.has("selectedEdges")) {
            require(step.path("selectedEdges").isArray(), location, "선택 간선 목록");
        }
        if (step.has("distances")) {
            JsonNode distances = step.path("distances");
            require(distances.isArray(), location, "거리 배열");
            for (JsonNode distance : distances) {
                require(distance.isNull() || distance.isInt(), location, "거리 값 형식");
            }
            require(distances.size() == size, location, "거리 배열 크기");
            require(step.path("source").isInt() && step.path("source").asInt() >= 0
                    && step.path("source").asInt() < size, location, "최단 경로 시작점");
        }
        if (step.has("distanceMatrix")) {
            JsonNode distanceMatrix = step.path("distanceMatrix");
            require(distanceMatrix.isArray() && distanceMatrix.size() == size, location, "거리 행렬 크기");
            for (JsonNode row : distanceMatrix) {
                require(row.isArray() && row.size() == size, location, "거리 행렬 행 크기");
                for (JsonNode distance : row) {
                    require(distance.isNull() || distance.isInt(), location, "거리 행렬 값 형식");
                }
            }
            if (step.has("intermediate")) {
                require(step.path("intermediate").isInt() && step.path("intermediate").asInt() >= 0
                        && step.path("intermediate").asInt() < size, location, "경유 정점 범위");
            }
        }
        for (String field : new String[]{"selectedEdges", "activeEdge"}) {
            if (!step.has(field)) {
                continue;
            }
            Iterable<JsonNode> pairs = step.path(field);
            if (field.equals("activeEdge")) {
                pairs = List.of(step.path(field));
            }
            for (JsonNode pair : pairs) {
                requireArray(pair, "int", location, field);
                require(pair.size() == 2, location, "간선 정점 쌍");
                int from = pair.get(0).asInt();
                int to = pair.get(1).asInt();
                require(from >= 0 && from < size && to >= 0 && to < size && edges[from][to], location, "선택·검사 간선 연결");
                if (field.equals("selectedEdges")) {
                    String key = directed ? from + "-" + to : Math.min(from, to) + "-" + Math.max(from, to);
                    require(selected.add(key), location, "선택 간선 중복");
                    total += weights.get(from).get(to).asInt();
                }
            }
        }
        if (step.has("selectedEdges")) {
            require(step.path("totalWeight").isInt() && step.path("totalWeight").asInt() == total, location, "선택 간선 비용 합");
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
