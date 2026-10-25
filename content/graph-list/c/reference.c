#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 50

typedef struct GraphNode {
    int vertex;
    struct GraphNode *link;
} GraphNode;

typedef struct {
    int n;
    GraphNode *adj_list[MAX_VERTICES];
} GraphType;

void init_graph(GraphType *g)
{
    g->n = 0;
    for (int i = 0; i < MAX_VERTICES; i++) {
        g->adj_list[i] = NULL;
    }
}

void insert_vertex(GraphType *g)
{
    if (g->n >= MAX_VERTICES) {
        fprintf(stderr, "정점 수 초과\n");
        exit(1);
    }
    g->adj_list[g->n] = NULL;
    g->n++;
}

void insert_edge(GraphType *g, int u, int v)
{
    if (u < 0 || v < 0 || u >= g->n || v >= g->n || u == v) {
        fprintf(stderr, "간선의 정점 오류\n");
        exit(1);
    }
    for (GraphNode *p = g->adj_list[u]; p != NULL; p = p->link) {
        if (p->vertex == v) {
            return;
        }
    }

    // 무방향 간선의 양쪽 노드 생성
    GraphNode *first = malloc(sizeof(GraphNode));
    GraphNode *second = malloc(sizeof(GraphNode));
    if (first == NULL || second == NULL) {
        free(first);
        free(second);
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    first->vertex = v;
    first->link = g->adj_list[u];
    g->adj_list[u] = first;
    second->vertex = u;
    second->link = g->adj_list[v];
    g->adj_list[v] = second;
}

void free_graph(GraphType *g)
{
    for (int i = 0; i < g->n; i++) {
        GraphNode *p = g->adj_list[i];
        while (p != NULL) {
            GraphNode *next = p->link;
            free(p);
            p = next;
        }
        g->adj_list[i] = NULL;
    }
    g->n = 0;
}
