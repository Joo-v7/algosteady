#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 50

typedef struct {
    int n;
    int adj_mat[MAX_VERTICES][MAX_VERTICES];
} GraphType;

void init_graph(GraphType *g);
void insert_vertex(GraphType *g);
void insert_edge(GraphType *g, int u, int v);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
