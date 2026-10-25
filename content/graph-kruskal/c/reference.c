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
    int edge_count;
    Edge edges[MAX_EDGES];
} GraphType;

// 경로 압축을 이용한 대표 원소 조회
static int set_find(int parent[], int vertex)
{
    if (parent[vertex] == -1) return vertex;
    parent[vertex] = set_find(parent, parent[vertex]);
    return parent[vertex];
}

static int compare_edges(const void *left, const void *right)
{
    const Edge *a = left;
    const Edge *b = right;
    return (a->weight > b->weight) - (a->weight < b->weight);
}

int kruskal(GraphType *g, Edge mst[])
{
    int parent[MAX_VERTICES];
    for (int i = 0; i < g->n; i++) parent[i] = -1;
    qsort(g->edges, g->edge_count, sizeof(Edge), compare_edges);

    // 서로 다른 집합을 잇는 간선만 선택
    int count = 0;
    for (int i = 0; i < g->edge_count && count < g->n - 1; i++) {
        Edge edge = g->edges[i];
        int start_root = set_find(parent, edge.start);
        int end_root = set_find(parent, edge.end);
        if (start_root == end_root) continue;
        mst[count++] = edge;
        parent[start_root] = end_root;
    }
    if (count != g->n - 1) return -1;
    return count;
}
