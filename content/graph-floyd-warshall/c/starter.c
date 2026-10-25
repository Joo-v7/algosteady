#include <stdio.h>

#define MAX_VERTICES 50
#define INF 1000000000

typedef struct {
    int n;
    int weight[MAX_VERTICES][MAX_VERTICES];
} GraphType;

// 모든 정점 쌍의 최단 거리 계산, 정상은 1·음수 사이클은 0 반환
int floyd_warshall(GraphType *g, int distance[][MAX_VERTICES]);
