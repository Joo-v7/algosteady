#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 50
#define MAX_EDGES (MAX_VERTICES * (MAX_VERTICES - 1) / 2)
#define INF 1000000000

typedef struct {
    int start;
    int end;
    int weight;
} Edge;

typedef struct {
    int n;
    int weight[MAX_VERTICES][MAX_VERTICES];
} GraphType;

// 아직 선택하지 않은 정점 중 최소 연결 비용 선택
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

int prim(GraphType *g, int start, Edge mst[])
{
    int distance[MAX_VERTICES];
    int selected[MAX_VERTICES] = {0};
    int parent[MAX_VERTICES];
    for (int i = 0; i < g->n; i++) {
        distance[i] = INF;
        parent[i] = -1;
    }
    distance[start] = 0;

    int count = 0;
    for (int i = 0; i < g->n; i++) {
        int vertex = get_min_vertex(g->n, distance, selected);
        if (vertex == -1 || distance[vertex] == INF) return -1;
        selected[vertex] = 1;
        if (parent[vertex] != -1) {
            mst[count++] = (Edge){parent[vertex], vertex, distance[vertex]};
        }

        // 선택 집합에서 각 미선택 정점으로 가는 최소 간선 갱신
        for (int next = 0; next < g->n; next++) {
            if (!selected[next] && g->weight[vertex][next] < distance[next]) {
                distance[next] = g->weight[vertex][next];
                parent[next] = vertex;
            }
        }
    }
    return count;
}
