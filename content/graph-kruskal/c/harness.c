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
                    
                }
            }
        } else if (strcmp(command, "EDGE") == 0) {
            int u;
            int v;
            int weight;
            if (scanf("%d %d %d", &u, &v, &weight) != 3 || u < 0 || v < 0
                    || u >= n || v >= n || u == v || weight < -1000000 || weight > 1000000) return 2;
            int found = 0;
            for (int i = 0; i < g.edge_count; i++) {
                Edge *edge = &g.edges[i];
                if ((edge->start == u && edge->end == v) || (edge->start == v && edge->end == u)) {
                    edge->weight = weight;
                    found = 1;
                    break;
                }
            }
            if (!found) g.edges[g.edge_count++] = (Edge){u, v, weight};
            original[u][v] = original[v][u] = weight;
        } else if (strcmp(command, "MST") == 0) {
            if (n == 0) return 2;
            
            Edge guarded[MAX_VERTICES + 1];
            for (int i = 0; i <= MAX_VERTICES; i++) guarded[i] = (Edge){-999, -999, -999};
            Edge *mst = &guarded[1];
            int count = kruskal(&g, mst);
            for (int i = 0; i <= MAX_VERTICES; i++) {
                if (i >= 1 && i < n) continue;
                if (guarded[i].start != -999 || guarded[i].end != -999 || guarded[i].weight != -999) fail();
            }
            if (g.n != n || g.edge_count < 0 || g.edge_count > MAX_EDGES) fail();
            int seen[MAX_VERTICES][MAX_VERTICES] = {0};
            for (int i = 0; i < g.edge_count; i++) {
                Edge edge = g.edges[i];
                if (edge.start < 0 || edge.start >= n || edge.end < 0 || edge.end >= n
                        || edge.start == edge.end || original[edge.start][edge.end] == INF
                        || edge.weight != original[edge.start][edge.end] || seen[edge.start][edge.end]) fail();
                seen[edge.start][edge.end] = seen[edge.end][edge.start] = 1;
            }
            for (int u = 0; u < n; u++) {
                for (int v = u + 1; v < n; v++) {
                    if (seen[u][v] != (original[u][v] != INF)) fail();
                }
            }
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
