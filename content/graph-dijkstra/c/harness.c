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
            for (int from = 0; from < MAX_VERTICES; from++) {
                for (int to = 0; to < MAX_VERTICES; to++) g.weight[from][to] = from == to ? 0 : INF;
            }
        } else if (strcmp(command, "EDGE") == 0) {
            int from;
            int to;
            int weight;
            if (scanf("%d %d %d", &from, &to, &weight) != 3 || from < 0 || from >= g.n
                    || to < 0 || to >= g.n || from == to || weight < 0 || weight > 1000) return 2;
            g.weight[from][to] = weight;
        } else if (strcmp(command, "PATH") == 0) {
            int start;
            if (scanf("%d", &start) != 1 || start < 0 || start >= g.n) return 2;
            GraphType original;
            memcpy(&original, &g, sizeof(g));
            int guarded[MAX_VERTICES + 2];
            for (int i = 0; i < MAX_VERTICES + 2; i++) guarded[i] = -987654321;
            int *distance = &guarded[1];
            dijkstra(&g, start, distance);
            if (memcmp(&g, &original, sizeof(g)) != 0) fail();
            for (int i = 0; i < MAX_VERTICES + 2; i++) {
                if (i >= 1 && i <= g.n) continue;
                if (guarded[i] != -987654321) fail();
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
