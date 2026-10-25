#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "최단 거리 배열·반환값·그래프 보존 검사 오류\n");
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
            
        } else if (strcmp(command, "EDGE") == 0) {
            int from;
            int to;
            int weight;
            if (scanf("%d %d %d", &from, &to, &weight) != 3 || from < 0 || from >= g.n
                    || to < 0 || to >= g.n || from == to || weight < -1000 || weight > 1000) return 2;
            int found = 0;
            for (int i = 0; i < g.edge_count; i++) {
                if (g.edges[i].from == from && g.edges[i].to == to) {
                    g.edges[i].weight = weight;
                    found = 1;
                    break;
                }
            }
            if (!found) g.edges[g.edge_count++] = (Edge){from, to, weight};
        } else if (strcmp(command, "PATH") == 0) {
            int start;
            if (scanf("%d", &start) != 1 || start < 0 || start >= g.n) return 2;
            GraphType original;
            memcpy(&original, &g, sizeof(g));
            int guarded[MAX_VERTICES + 2];
            for (int i = 0; i < MAX_VERTICES + 2; i++) guarded[i] = -987654321;
            int *distance = &guarded[1];
            int status = bellman_ford(&g, start, distance);
            if (memcmp(&g, &original, sizeof(g)) != 0) fail();
            for (int i = 0; i < MAX_VERTICES + 2; i++) {
                if (i >= 1 && i <= g.n) continue;
                if (guarded[i] != -987654321) fail();
            }
            if (status != 0 && status != 1) fail();
            if (status == 0) {
                printf("NEGATIVE_CYCLE\n");
                continue;
            }
            printf("DIST");
            for (int i = 0; i < g.n; i++) {
                if (distance[i] == INF) printf(" INF");
                else printf(" %d", distance[i]);
            }
            printf("\n");
        } else return 2;
    }
    return 0;
}
