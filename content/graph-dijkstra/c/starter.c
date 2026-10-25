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

// 시작점에서 각 정점까지 최단 거리 계산
void dijkstra(GraphType *g, int start, int distance[]);
