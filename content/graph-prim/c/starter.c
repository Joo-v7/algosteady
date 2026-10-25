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

// 선택 간선을 mst에 저장하고 간선 수 반환, 연결 불가 시 -1
int prim(GraphType *g, int start, Edge mst[]);
