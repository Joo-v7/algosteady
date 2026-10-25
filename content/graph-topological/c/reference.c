#include <stdio.h>

#define MAX_VERTICES 50

typedef struct {
    int n;
    int adj_mat[MAX_VERTICES][MAX_VERTICES];
} GraphType;

int topological_sort(GraphType *g, int order[])
{
    int in_degree[MAX_VERTICES] = {0};
    int queue[MAX_VERTICES];
    int front = 0;
    int rear = 0;
    int count = 0;

    // 각 정점으로 들어오는 간선 수 계산
    for (int from = 0; from < g->n; from++) {
        for (int to = 0; to < g->n; to++) {
            if (g->adj_mat[from][to]) in_degree[to]++;
        }
    }
    for (int v = 0; v < g->n; v++) {
        if (in_degree[v] == 0) queue[rear++] = v;
    }

    // 진입 차수가 0인 정점 처리와 후속 정점의 진입 차수 감소
    while (front < rear) {
        int vertex = queue[front++];
        order[count++] = vertex;
        for (int next = 0; next < g->n; next++) {
            if (g->adj_mat[vertex][next]) {
                in_degree[next]--;
                if (in_degree[next] == 0) queue[rear++] = next;
            }
        }
    }
    if (count != g->n) return -1;
    return count;
}
