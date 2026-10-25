#include <stdio.h>

#define MAX_VERTICES 50
#define INF 1000000000

typedef struct {
    int n;
    int weight[MAX_VERTICES][MAX_VERTICES];
} GraphType;

int floyd_warshall(GraphType *g, int distance[][MAX_VERTICES])
{
    for (int i = 0; i < g->n; i++) {
        for (int j = 0; j < g->n; j++) distance[i][j] = g->weight[i][j];
    }

    // 허용하는 경유 정점을 하나씩 추가하며 모든 정점 쌍 갱신
    for (int k = 0; k < g->n; k++) {
        for (int i = 0; i < g->n; i++) {
            for (int j = 0; j < g->n; j++) {
                if (distance[i][k] != INF && distance[k][j] != INF
                        && distance[i][k] + distance[k][j] < distance[i][j]) {
                    distance[i][j] = distance[i][k] + distance[k][j];
                }
            }
        }

        // 음수 사이클 발견 시 불필요한 거리 감소 중단
        for (int v = 0; v < g->n; v++) {
            if (distance[v][v] < 0) return 0;
        }
    }
    return 1;
}
