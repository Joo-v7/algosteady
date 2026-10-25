#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int allocation_count;
static int release_count;

/* 사용자 노드의 할당·해제 호출 수 확인 */
static void *allocate_node(size_t size)
{
    void *node = malloc(size);
    if (node != NULL) allocation_count++;
    return node;
}

static void release_node(void *node)
{
    if (node != NULL) release_count++;
    free(node);
}

#define malloc allocate_node
#define free release_node
#include "submission.c"
#undef malloc
#undef free


static void fail(void)
{
    fprintf(stderr, "그래프 상태·연결·해제 검사 오류\n");
    exit(2);
}

static void check(GraphType *g)
{
    if (g->n < 0 || g->n > MAX_VERTICES) fail();
    for (int i = 0; i < MAX_VERTICES; i++) {
        if (i >= g->n && g->adj_list[i] != NULL) fail();
        int seen[MAX_VERTICES] = {0};
        int count = 0;
        for (GraphNode *p = g->adj_list[i]; p != NULL; p = p->link) {
            if (++count > g->n || p->vertex < 0 || p->vertex >= g->n || p->vertex == i || seen[p->vertex]) fail();
            seen[p->vertex] = 1;
            int reverse = 0;
            int reverse_count = 0;
            for (GraphNode *q = g->adj_list[p->vertex]; q != NULL; q = q->link) {
                if (++reverse_count > g->n) fail();
                if (q->vertex == i) reverse = 1;
            }
            if (!reverse) fail();
        }
    }
}

int main(void)
{
    GraphType graphs[2];
    int current = 0;
    char command[16];
    for (int i = 0; i < 2; i++) {
        memset(&graphs[i], 0x55, sizeof(GraphType));
        init_graph(&graphs[i]);
        if (graphs[i].n != 0) fail();
        check(&graphs[i]);
    }
    while (scanf("%15s", command) == 1) {
        GraphType *g = &graphs[current];
        if (strcmp(command, "USE") == 0) {
            if (scanf("%d", &current) != 1 || current < 0 || current > 1) return 2;
        } else if (strcmp(command, "INIT") == 0) {
            int n;
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_VERTICES) return 2;
            free_graph(g);
            init_graph(g);
            if (g->n != 0) fail();
            check(g);
            for (int i = 0; i < n; i++) {
                insert_vertex(g);
                if (g->n != i + 1) fail();
            }
        } else if (strcmp(command, "VERTEX") == 0) {
            int before = g->n;
            insert_vertex(g);
            if (g->n != before + 1) fail();
        } else if (strcmp(command, "EDGE") == 0) {
            int u;
            int v;
            if (scanf("%d %d", &u, &v) != 2) return 2;
            insert_edge(g, u, v);
        } else if (strcmp(command, "SHOW") == 0) {
            printf("LIST %d\n", g->n);
            for (int i = 0; i < g->n; i++) {
                printf("%d:", i);
                for (GraphNode *p = g->adj_list[i]; p != NULL; p = p->link) printf(" %d", p->vertex);
                printf("\n");
            }
        } else if (strcmp(command, "FREE") == 0) {
            free_graph(g);
            if (g->n != 0) fail();
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else return 2;
        check(&graphs[0]);
        check(&graphs[1]);
    }
    for (int i = 0; i < 2; i++) {
        free_graph(&graphs[i]);
        if (graphs[i].n != 0) fail();
        check(&graphs[i]);
    }
    if (allocation_count != release_count) fail();
    return 0;
}
