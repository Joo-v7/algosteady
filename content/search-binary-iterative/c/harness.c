#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "탐색 결과·범위·배열 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    int n = 0;
    int key;
    int low;
    int high;
    char command[16];
    memset(list, 0x55, sizeof(list));
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "LOAD") == 0) {
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_SIZE) return 2;
            memset(list, 0x55, sizeof(list));
            for (int i = 0; i < n; i++) {
                if (scanf("%d", &list[i]) != 1 || list[i] < -1000000 || list[i] > 1000000) return 2;
                if (i > 0 && list[i] < list[i - 1]) return 2;
            }
        } else if (strcmp(command, "SEARCH") == 0 || strcmp(command, "CHECK") == 0) {
            if (scanf("%d %d %d", &key, &low, &high) != 3 || low < 0 || low > n || high < -1 || high >= n) return 2;
            int exists = 0;
            for (int i = low; i <= high; i++) {
                if (list[i] == key) exists = 1;
            }
            int original[MAX_SIZE];
            memcpy(original, list, sizeof(list));
            int actual = search_binary(key, low, high);
            if (memcmp(original, list, sizeof(list)) != 0) fail();
            if (!exists) {
                if (actual != -1) fail();
            } else if (actual < low || actual > high || list[actual] != key) fail();
            if (strcmp(command, "CHECK") == 0) {
                if (actual == -1) printf("ABSENT\n");
                else printf("FOUND %d\n", key);
            } else printf("SEARCH %d\n", actual);
        } else return 2;
    }
    return 0;
}
