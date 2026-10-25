#include <stdio.h>
#include <stdlib.h>

#define MAX_QUEUE_SIZE 5

typedef int element;

typedef struct {
    element data[MAX_QUEUE_SIZE];
    int front;
    int rear;
} DequeType;

void init_deque(DequeType *q);
int is_empty(DequeType *q);
int is_full(DequeType *q);
void add_front(DequeType *q, element item);
void add_rear(DequeType *q, element item);
element delete_front(DequeType *q);
element delete_rear(DequeType *q);
element get_front(DequeType *q);
element get_rear(DequeType *q);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
