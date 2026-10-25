#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "순회 방문 상태·그래프 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    GraphType g;
    init_graph(&g);
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "INIT") == 0) {
            int n;
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_VERTICES) return 2;
            init_graph(&g);
            memset(visited, 0, sizeof(visited));
            for (int i = 0; i < n; i++) insert_vertex(&g);
        } else if (strcmp(command, "EDGE") == 0) {
            int u;
            int v;
            if (scanf("%d %d", &u, &v) != 2) return 2;
            insert_edge(&g, u, v);
        } else if (strcmp(command, "TRAVERSE") == 0 || strcmp(command, "CONTINUE") == 0) {
            int start;
            if (scanf("%d", &start) != 1 || start < 0 || start >= g.n) return 2;
            if (strcmp(command, "TRAVERSE") == 0) memset(visited, 0, sizeof(visited));
            if (visited[start]) return 2;
            int expected[MAX_VERTICES];
            memcpy(expected, visited, sizeof(visited));
            int reachable[MAX_VERTICES] = {0};
            int queue[MAX_VERTICES];
            int front = 0;
            int rear = 0;
            queue[rear++] = start;
            reachable[start] = 1;
            while (front < rear) {
                int vertex = queue[front++];
                expected[vertex] = 1;
                for (int next = 0; next < g.n; next++) {
                    if (g.adj_mat[vertex][next] && !reachable[next]) {
                        reachable[next] = 1;
                        queue[rear++] = next;
                    }
                }
            }
            GraphType original;
            memcpy(&original, &g, sizeof(g));
            printf("BFS ");
            bfs_mat(&g, start);
            printf("\n");
            if (memcmp(&original, &g, sizeof(g)) != 0 || memcmp(expected, visited, sizeof(visited)) != 0) fail();
        } else if (strcmp(command, "VISITED") == 0) {
            printf("VISITED");
            for (int i = 0; i < g.n; i++) printf(" %d", visited[i]);
            printf("\n");
        } else return 2;
    }
    return 0;
}
