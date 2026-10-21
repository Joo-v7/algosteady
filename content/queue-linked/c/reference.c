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

void init(LinkedQueueType *q)
{
    q->front = NULL;
    q->rear = NULL;
}

int is_empty(LinkedQueueType *q)
{
    return (q->front == NULL);
}

int is_full(LinkedQueueType *q)
{
    return 0;
}

void enqueue(LinkedQueueType *q, element item)
{
    QueueNode *node = malloc(sizeof(QueueNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = item;
    node->link = NULL;
    if (is_empty(q)) {
        q->front = node;
        q->rear = node;
    } else {
        q->rear->link = node;
        q->rear = node;
    }
}

element dequeue(LinkedQueueType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "큐 공백 에러\n");
        exit(1);
    }
    QueueNode *removed = q->front;
    element item = removed->data;
    q->front = removed->link;
    if (q->front == NULL) q->rear = NULL;
    free(removed);
    return item;
}

void print_queue(LinkedQueueType *q)
{
    for (QueueNode *p = q->front; p != NULL; p = p->link) {
        printf("%d->", p->data);
    }
    printf("NULL\n");
}
