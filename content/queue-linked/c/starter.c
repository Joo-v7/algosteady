#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct QueueNode {
    element data;
    struct QueueNode *link;
} QueueNode;

typedef struct {
    QueueNode *front;
    QueueNode *rear;
} LinkedQueueType;

void init(LinkedQueueType *q);
int is_empty(LinkedQueueType *q);
int is_full(LinkedQueueType *q);
void enqueue(LinkedQueueType *q, element item);
element dequeue(LinkedQueueType *q);
void print_queue(LinkedQueueType *q);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
