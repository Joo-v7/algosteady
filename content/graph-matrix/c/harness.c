#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "그래프 상태·연결·해제 검사 오류\n");
    exit(2);
}

static void check(GraphType *g)
{
    if (g->n < 0 || g->n > MAX_VERTICES) fail();
    for (int i = 0; i < MAX_VERTICES; i++) {
        for (int j = 0; j < MAX_VERTICES; j++) {
            int value = g->adj_mat[i][j];
            if ((i >= g->n || j >= g->n || i == j) && value != 0) fail();
            if (value != 0 && value != 1) fail();
            if (value != g->adj_mat[j][i]) fail();
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
            printf("MATRIX %d\n", g->n);
            for (int i = 0; i < g->n; i++) {
                for (int j = 0; j < g->n; j++) printf(j == 0 ? "%d" : " %d", g->adj_mat[i][j]);
                printf("\n");
            }

        } else return 2;
        check(&graphs[0]);
        check(&graphs[1]);
    }

    return 0;
}
