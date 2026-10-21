#include <stdio.h>
#include <stdlib.h>

#define MAX_QUEUE_SIZE 5

typedef int element;

typedef struct {
    element data[MAX_QUEUE_SIZE];
    int front;
    int rear;
} DequeType;

void init_deque(DequeType *q)
{
    q->front = 0;
    q->rear = 0;
}

int is_empty(DequeType *q)
{
    return (q->front == q->rear);
}

int is_full(DequeType *q)
{
    return ((q->rear + 1) % MAX_QUEUE_SIZE == q->front);
}

void add_front(DequeType *q, element item)
{
    if (is_full(q)) {
        fprintf(stderr, "덱 포화 에러\n");
        exit(1);
    }
    q->data[q->front] = item;
    q->front = (q->front - 1 + MAX_QUEUE_SIZE) % MAX_QUEUE_SIZE;
}

void add_rear(DequeType *q, element item)
{
    if (is_full(q)) {
        fprintf(stderr, "덱 포화 에러\n");
        exit(1);
    }
    q->rear = (q->rear + 1) % MAX_QUEUE_SIZE;
    q->data[q->rear] = item;
}

element delete_front(DequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    q->front = (q->front + 1) % MAX_QUEUE_SIZE;
    return q->data[q->front];
}

element delete_rear(DequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    int previous = q->rear;
    q->rear = (q->rear - 1 + MAX_QUEUE_SIZE) % MAX_QUEUE_SIZE;
    return q->data[previous];
}

element get_front(DequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    return q->data[(q->front + 1) % MAX_QUEUE_SIZE];
}

element get_rear(DequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    return q->data[q->rear];
}
