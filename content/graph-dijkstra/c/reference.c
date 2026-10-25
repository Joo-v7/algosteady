#include <stdio.h>

#define MAX_VERTICES 50
#define INF 1000000000

typedef struct {
    int n;
    int weight[MAX_VERTICES][MAX_VERTICES];
} GraphType;

// 미확정 정점 중 최소 거리 선택
static int get_min_vertex(int n, const int distance[], const int selected[])
{
    int vertex = -1;
    for (int i = 0; i < n; i++) {
        if (!selected[i] && (vertex == -1 || distance[i] < distance[vertex])) {
            vertex = i;
        }
    }
    return vertex;
}

void dijkstra(GraphType *g, int start, int distance[])
{
    int selected[MAX_VERTICES] = {0};
    for (int i = 0; i < g->n; i++) distance[i] = INF;
    distance[start] = 0;

    for (int i = 0; i < g->n; i++) {
        int vertex = get_min_vertex(g->n, distance, selected);
        if (vertex == -1 || distance[vertex] == INF) break;
        selected[vertex] = 1;

        // 확정한 정점을 경유하는 더 짧은 경로 반영
        for (int next = 0; next < g->n; next++) {
            if (!selected[next] && g->weight[vertex][next] != INF
                    && distance[vertex] + g->weight[vertex][next] < distance[next]) {
                distance[next] = distance[vertex] + g->weight[vertex][next];
            }
        }
    }
}
