#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 50

typedef struct {
    int n;
    int adj_mat[MAX_VERTICES][MAX_VERTICES];
} GraphType;

/* 그래프 생성 연산은 제공 */
void init_graph(GraphType *g)
{
    g->n = 0;
    for (int i = 0; i < MAX_VERTICES; i++) {
        for (int j = 0; j < MAX_VERTICES; j++) {
            g->adj_mat[i][j] = 0;
        }
    }
}

void insert_vertex(GraphType *g)
{
    if (g->n >= MAX_VERTICES) {
        fprintf(stderr, "정점 수 초과\n");
        exit(1);
    }
    g->n++;
}

void insert_edge(GraphType *g, int u, int v)
{
    if (u < 0 || v < 0 || u >= g->n || v >= g->n || u == v) {
        fprintf(stderr, "간선의 정점 오류\n");
        exit(1);
    }
    g->adj_mat[u][v] = 1;
    g->adj_mat[v][u] = 1;
}

int visited[MAX_VERTICES];

void dfs_mat(GraphType *g, int v)
{
    visited[v] = 1;
    printf("%d ", v);
    for (int w = 0; w < g->n; w++) {
        if (g->adj_mat[v][w] && !visited[w]) {
            dfs_mat(g, w);
        }
    }
}
