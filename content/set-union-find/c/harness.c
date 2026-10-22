#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "대표 원소·부모 연결·경로 압축 검사 오류\n");
    exit(2);
}

/* 예상 경로를 먼저 수집한 뒤 경로 전체를 대표 원소에 연결한다. */
static int expected_find(int curr, int expected[])
{
    int path[MAX_VERTICES];
    int count = 0;
    int root = curr;
    while (expected[root] != -1) {
        path[count++] = root;
        root = expected[root];
    }
    for (int i = 0; i < count; i++) expected[path[i]] = root;
    return root;
}

static void validate(int n, int expected[])
{
    for (int i = 0; i < n; i++) {
        if (parent[i] < -1 || parent[i] >= n || parent[i] != expected[i]) fail();
        int curr = i;
        int count = 0;
        while (parent[curr] != -1) {
            curr = parent[curr];
            if (curr < 0 || curr >= n || ++count >= n) fail();
        }
    }
}

int main(void)
{
    int expected[MAX_VERTICES] = {0};
    int n = 0;
    int a;
    int b;
    char command[16];
    memset(parent, 0x55, sizeof(parent));
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "INIT") == 0) {
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_VERTICES) return 2;
            set_init(n);
            for (int i = 0; i < n; i++) expected[i] = -1;
        } else if (strcmp(command, "FIND") == 0) {
            if (scanf("%d", &a) != 1 || a < 0 || a >= n) return 2;
            int root = expected_find(a, expected);
            int actual = set_find(a);
            if (actual != root) fail();
            printf("FIND %d\n", actual);
        } else if (strcmp(command, "UNION") == 0 || strcmp(command, "SAME") == 0) {
            if (scanf("%d %d", &a, &b) != 2 || a < 0 || a >= n || b < 0 || b >= n) return 2;
            int root1 = expected_find(a, expected);
            int root2 = expected_find(b, expected);
            if (strcmp(command, "UNION") == 0) {
                set_union(a, b);
                if (root1 != root2) expected[root1] = root2;
            } else {
                int actual1 = set_find(a);
                int actual2 = set_find(b);
                if (actual1 != root1 || actual2 != root2) fail();
                printf("SAME %d\n", actual1 == actual2);
            }
        } else if (strcmp(command, "STATE") == 0) {
            printf("PARENT");
            for (int i = 0; i < n; i++) printf(" %d", parent[i]);
            printf("\n");
        } else if (strcmp(command, "GROUPS") == 0) {
            int groups = 0;
            for (int i = 0; i < n; i++) {
                if (parent[i] == -1) groups++;
            }
            printf("GROUPS %d\n", groups);
        } else return 2;
        validate(n, expected);
    }
    return 0;
}
