#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct DequeNode {
    element data;
    struct DequeNode *llink;
    struct DequeNode *rlink;
} DequeNode;

typedef struct {
    DequeNode *front;
    DequeNode *rear;
} LinkedDequeType;

void init_deque(LinkedDequeType *q);
int is_empty(LinkedDequeType *q);
int is_full(LinkedDequeType *q);
void add_front(LinkedDequeType *q, element item);
void add_rear(LinkedDequeType *q, element item);
element delete_front(LinkedDequeType *q);
element delete_rear(LinkedDequeType *q);
element get_front(LinkedDequeType *q);
element get_rear(LinkedDequeType *q);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
