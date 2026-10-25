#include <string.h>
#include "submission.c"

static int original[MAX_VERTICES][MAX_VERTICES];

static void fail(void)
{
    fprintf(stderr, "신장 트리 간선·반환값·그래프 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    GraphType g = {0};
    int n = 0;
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "INIT") == 0) {
            if (scanf("%d", &n) != 1 || n < 1 || n > MAX_VERTICES) return 2;
            memset(&g, 0, sizeof(g));
            g.n = n;
            for (int u = 0; u < MAX_VERTICES; u++) {
                for (int v = 0; v < MAX_VERTICES; v++) {
                    original[u][v] = u == v ? 0 : INF;
                    g.weight[u][v] = original[u][v];
                }
            }
        } else if (strcmp(command, "EDGE") == 0) {
            int u;
            int v;
            int weight;
            if (scanf("%d %d %d", &u, &v, &weight) != 3 || u < 0 || v < 0
                    || u >= n || v >= n || u == v || weight < -1000000 || weight > 1000000) return 2;
            g.weight[u][v] = g.weight[v][u] = weight;
            original[u][v] = original[v][u] = weight;
        } else if (strcmp(command, "MST") == 0) {
            if (n == 0) return 2;
            int start;
            if (scanf("%d", &start) != 1 || start < 0 || start >= n) return 2;
            Edge guarded[MAX_VERTICES + 1];
            for (int i = 0; i <= MAX_VERTICES; i++) guarded[i] = (Edge){-999, -999, -999};
            Edge *mst = &guarded[1];
            int count = prim(&g, start, mst);
            for (int i = 0; i <= MAX_VERTICES; i++) {
                if (i >= 1 && i < n) continue;
                if (guarded[i].start != -999 || guarded[i].end != -999 || guarded[i].weight != -999) fail();
            }
            if (g.n != n || memcmp(g.weight, original, sizeof(original)) != 0) fail();
            if (count == -1) {
                printf("DISCONNECTED\n");
                continue;
            }
            if (count != n - 1) fail();
            int roots[MAX_VERTICES];
            for (int i = 0; i < n; i++) roots[i] = i;
            int total = 0;
            for (int i = 0; i < count; i++) {
                Edge edge = mst[i];
                if (edge.start < 0 || edge.start >= n || edge.end < 0 || edge.end >= n
                        || edge.start == edge.end || original[edge.start][edge.end] == INF
                        || edge.weight != original[edge.start][edge.end]) fail();
                int a = edge.start;
                int b = edge.end;
                while (roots[a] != a) a = roots[a];
                while (roots[b] != b) b = roots[b];
                if (a == b) fail();
                roots[a] = b;
                total += edge.weight;
            }
            printf("MST %d %d\n", count, total);
        } else return 2;
    }
    return 0;
}
