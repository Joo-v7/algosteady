#include <stdio.h>

#define MAX_VERTICES 50
#define INF 1000000000

#define MAX_EDGES (MAX_VERTICES * (MAX_VERTICES - 1))

typedef struct {
    int from;
    int to;
    int weight;
} Edge;

typedef struct {
    int n;
    int edge_count;
    Edge edges[MAX_EDGES];
} GraphType;

int bellman_ford(GraphType *g, int start, int distance[])
{
    for (int i = 0; i < g->n; i++) distance[i] = INF;
    distance[start] = 0;

    // 최대 n-1회 전체 간선 완화
    for (int pass = 0; pass < g->n - 1; pass++) {
        int changed = 0;
        for (int i = 0; i < g->edge_count; i++) {
            Edge edge = g->edges[i];
            if (distance[edge.from] != INF
                    && distance[edge.from] + edge.weight < distance[edge.to]) {
                distance[edge.to] = distance[edge.from] + edge.weight;
                changed = 1;
            }
        }
        if (!changed) break;
    }

    // 시작점에서 도달 가능한 음수 사이클 검사
    for (int i = 0; i < g->edge_count; i++) {
        Edge edge = g->edges[i];
        if (distance[edge.from] != INF
                && distance[edge.from] + edge.weight < distance[edge.to]) {
            return 0;
        }
    }
    return 1;
}
