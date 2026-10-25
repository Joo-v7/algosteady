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

void init_deque(LinkedDequeType *q)
{
    q->front = NULL;
    q->rear = NULL;
}

int is_empty(LinkedDequeType *q)
{
    return (q->front == NULL);
}

int is_full(LinkedDequeType *q)
{
    return 0;
}

void add_front(LinkedDequeType *q, element item)
{
    DequeNode *node = malloc(sizeof(DequeNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = item;
    node->llink = NULL;
    node->rlink = q->front;
    if (is_empty(q)) q->rear = node;
    else q->front->llink = node;
    q->front = node;
}

void add_rear(LinkedDequeType *q, element item)
{
    DequeNode *node = malloc(sizeof(DequeNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = item;
    node->llink = q->rear;
    node->rlink = NULL;
    if (is_empty(q)) q->front = node;
    else q->rear->rlink = node;
    q->rear = node;
}

element delete_front(LinkedDequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    DequeNode *removed = q->front;
    element item = removed->data;
    q->front = removed->rlink;
    if (q->front == NULL) q->rear = NULL;
    else q->front->llink = NULL;
    free(removed);
    return item;
}

element delete_rear(LinkedDequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    DequeNode *removed = q->rear;
    element item = removed->data;
    q->rear = removed->llink;
    if (q->rear == NULL) q->front = NULL;
    else q->rear->rlink = NULL;
    free(removed);
    return item;
}

element get_front(LinkedDequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    return q->front->data;
}

element get_rear(LinkedDequeType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "덱 공백 에러\n");
        exit(1);
    }
    return q->rear->data;
}
