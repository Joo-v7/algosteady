#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 50

typedef struct {
    int n;
    int adj_mat[MAX_VERTICES][MAX_VERTICES];
} GraphType;

/* 그래프 생성 연산은 제공 */
void init_graph(GraphType *g)
{
    g->n = 0;
    for (int i = 0; i < MAX_VERTICES; i++) {
        for (int j = 0; j < MAX_VERTICES; j++) {
            g->adj_mat[i][j] = 0;
        }
    }
}

void insert_vertex(GraphType *g)
{
    if (g->n >= MAX_VERTICES) {
        fprintf(stderr, "정점 수 초과\n");
        exit(1);
    }
    g->n++;
}

void insert_edge(GraphType *g, int u, int v)
{
    if (u < 0 || v < 0 || u >= g->n || v >= g->n || u == v) {
        fprintf(stderr, "간선의 정점 오류\n");
        exit(1);
    }
    g->adj_mat[u][v] = 1;
    g->adj_mat[v][u] = 1;
}

int visited[MAX_VERTICES];

#define MAX_QUEUE_SIZE (MAX_VERTICES + 1)

typedef struct {
    int data[MAX_QUEUE_SIZE];
    int front;
    int rear;
} QueueType;

/* 순회에 사용할 큐 연산은 제공 */
void init_queue(QueueType *q)
{
    q->front = q->rear = 0;
}

int is_empty(QueueType *q)
{
    return q->front == q->rear;
}

void enqueue(QueueType *q, int item)
{
    if ((q->rear + 1) % MAX_QUEUE_SIZE == q->front) {
        fprintf(stderr, "큐 포화 오류\n");
        exit(1);
    }
    q->rear = (q->rear + 1) % MAX_QUEUE_SIZE;
    q->data[q->rear] = item;
}

int dequeue(QueueType *q)
{
    if (is_empty(q)) {
        fprintf(stderr, "큐 공백 오류\n");
        exit(1);
    }
    q->front = (q->front + 1) % MAX_QUEUE_SIZE;
    return q->data[q->front];
}

void bfs_mat(GraphType *g, int v);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
