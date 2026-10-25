#include <stdio.h>
#include <stdlib.h>

#define MAX_QUEUE_SIZE 5

typedef int element;

typedef struct {
    int front;
    int rear;
    element data[MAX_QUEUE_SIZE];
} QueueType;

void init_queue(QueueType *q)
{
    q->front = -1;
    q->rear = -1;
}

int is_empty(QueueType *q)
{
    return (q->front == q->rear);
}

int is_full(QueueType *q)
{
    return (q->rear == MAX_QUEUE_SIZE - 1);
}

void enqueue(QueueType *q, element item)
{
    if (is_full(q)) {
        fprintf(stderr, "큐 포화 에러\n");
        exit(1);
    }
    q->data[++(q->rear)] = item;
}

element dequeue(QueueType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "큐 공백 에러\n");
        exit(1);
    }
    return q->data[++(q->front)];
}
