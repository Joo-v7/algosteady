#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "위상 순서·반환값·그래프 보존 검사 오류\n");
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
            if (scanf("%d %d", &from, &to) != 2 || from < 0 || from >= g.n
                    || to < 0 || to >= g.n || from == to) return 2;
            g.adj_mat[from][to] = 1;
        } else if (strcmp(command, "SORT") == 0) {
            if (g.n == 0) return 2;
            GraphType original;
            memcpy(&original, &g, sizeof(g));
            int guarded[MAX_VERTICES + 2];
            for (int i = 0; i < MAX_VERTICES + 2; i++) guarded[i] = -987654321;
            int *order = &guarded[1];
            int count = topological_sort(&g, order);
            if (memcmp(&g, &original, sizeof(g)) != 0) fail();
            for (int i = 0; i < MAX_VERTICES + 2; i++) {
                if (i >= 1 && i <= g.n) continue;
                if (guarded[i] != -987654321) fail();
            }
            if (count == -1) {
                printf("CYCLE\n");
                continue;
            }
            if (count != g.n) fail();

            // 중복·누락 없이 모든 간선의 선후 관계 확인
            int position[MAX_VERTICES];
            for (int v = 0; v < g.n; v++) position[v] = -1;
            for (int i = 0; i < count; i++) {
                int vertex = order[i];
                if (vertex < 0 || vertex >= g.n || position[vertex] != -1) fail();
                position[vertex] = i;
            }
            for (int from = 0; from < g.n; from++) {
                for (int to = 0; to < g.n; to++) {
                    if (g.adj_mat[from][to] && position[from] >= position[to]) fail();
                }
            }
            printf("ORDER_VALID %d\n", count);
        } else return 2;
    }
    return 0;
}
