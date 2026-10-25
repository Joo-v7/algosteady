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

// 시작점에서 각 정점까지 최단 거리 계산
int bellman_ford(GraphType *g, int start, int distance[]);
