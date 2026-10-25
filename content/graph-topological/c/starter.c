#include <stdio.h>

#define MAX_VERTICES 50

typedef struct {
    int n;
    int adj_mat[MAX_VERTICES][MAX_VERTICES];
} GraphType;

// 위상 순서를 order에 저장하고 정점 수 반환, 사이클은 -1 반환
int topological_sort(GraphType *g, int order[]);
