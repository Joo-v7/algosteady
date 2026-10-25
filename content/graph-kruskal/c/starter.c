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

// 선택 간선을 mst에 저장하고 간선 수 반환, 연결 불가 시 -1
int kruskal(GraphType *g, Edge mst[]);
