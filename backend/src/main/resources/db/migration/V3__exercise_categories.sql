CREATE TABLE exercise_category (
    id VARCHAR(255) PRIMARY KEY
  , label VARCHAR(255) NOT NULL
  , display_order INTEGER NOT NULL UNIQUE
);

CREATE TABLE exercise_category_member (
    category_id VARCHAR(255) NOT NULL REFERENCES exercise_category (id)
  , content_category VARCHAR(255) PRIMARY KEY
);

INSERT INTO exercise_category (id, label, display_order)
VALUES ('recursion', '재귀', 1)
     , ('stack', '스택', 2)
     , ('queue', '큐', 3)
     , ('deque', '덱', 4)
     , ('linked-list', '연결 리스트', 5)
     , ('tree', '트리', 6)
     , ('heap', '힙', 7)
     , ('hash', '해싱', 8)
     , ('set', '집합', 9)
     , ('sort', '정렬', 10)
     , ('search', '탐색', 11)
     , ('graph', '그래프', 12)
     , ('string', '문자열', 13)
     , ('problem-solving', '문제 해결 기법', 14);

INSERT INTO exercise_category_member (category_id, content_category)
VALUES ('recursion', 'recursion')
     , ('stack', 'stack')
     , ('queue', 'queue')
     , ('deque', 'deque')
     , ('linked-list', 'linked-list')
     , ('tree', 'tree')
     , ('heap', 'heap')
     , ('hash', 'hash')
     , ('set', 'set')
     , ('sort', 'sort')
     , ('search', 'search')
     , ('graph', 'graph')
     , ('string', 'string')
     , ('string', 'trie')
     , ('problem-solving', 'exhaustive')
     , ('problem-solving', 'backtracking')
     , ('problem-solving', 'greedy')
     , ('problem-solving', 'dp')
     , ('problem-solving', 'two-pointer')
     , ('problem-solving', 'sliding-window');
