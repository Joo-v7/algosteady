#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "거리 행렬·반환값·그래프 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    GraphType g = {0};
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "INIT") == 0) {
            int n;
            if (scanf("%d", &n) != 1 || n < 1 || n > MAX_VERTICES) return 2;
            memset(&g, 0, sizeof(g));
            g.n = n;
            for (int i = 0; i < MAX_VERTICES; i++) {
                for (int j = 0; j < MAX_VERTICES; j++) g.weight[i][j] = i == j ? 0 : INF;
            }
        } else if (strcmp(command, "EDGE") == 0) {
            int from;
            int to;
            int weight;
            if (scanf("%d %d %d", &from, &to, &weight) != 3 || from < 0 || from >= g.n
                    || to < 0 || to >= g.n || from == to || weight < -1000 || weight > 1000) return 2;
            g.weight[from][to] = weight;
        } else if (strcmp(command, "ALL") == 0) {
            if (g.n == 0) return 2;
            GraphType original;
            memcpy(&original, &g, sizeof(g));
            struct {
                int before;
                int distance[MAX_VERTICES][MAX_VERTICES];
                int after;
            } guarded;
            guarded.before = guarded.after = -987654321;
            for (int i = 0; i < MAX_VERTICES; i++) {
                for (int j = 0; j < MAX_VERTICES; j++) guarded.distance[i][j] = -987654321;
            }
            int status = floyd_warshall(&g, guarded.distance);
            if (memcmp(&g, &original, sizeof(g)) != 0 || guarded.before != -987654321
                    || guarded.after != -987654321 || (status != 0 && status != 1)) fail();
            for (int i = 0; i < MAX_VERTICES; i++) {
                for (int j = 0; j < MAX_VERTICES; j++) {
                    if ((i >= g.n || j >= g.n) && guarded.distance[i][j] != -987654321) fail();
                }
            }
            if (status == 0) {
                printf("NEGATIVE_CYCLE\n");
                continue;
            }
            printf("DIST\n");
            for (int i = 0; i < g.n; i++) {
                for (int j = 0; j < g.n; j++) {
                    if (j > 0) printf(" ");
                    if (guarded.distance[i][j] == INF) printf("INF");
                    else printf("%d", guarded.distance[i][j]);
                }
                printf("\n");
            }
        } else return 2;
    }
    return 0;
}
