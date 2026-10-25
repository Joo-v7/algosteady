#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "입력 배열 보존·배열 경계 검사 오류\n");
    exit(2);
}

int main(void)
{
    struct { int before; int values[MAX_SIZE]; int after; } data;
    data.before = data.after = -987654321;
    int n = 0;
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "LOAD") == 0) {
            if (scanf("%d", &n) != 1 || n < 1 || n > MAX_SIZE) return 2;
            memset(data.values, 0x55, sizeof(data.values));
            for (int i = 0; i < n; i++) {
                if (scanf("%d", &data.values[i]) != 1 || data.values[i] < -1000000 || data.values[i] > 1000000) return 2;
            }
        } else if (strcmp(command, "MAX") == 0) {
            int k;
            if (scanf("%d", &k) != 1 || k < 1 || k > n) return 2;
            int original[MAX_SIZE];
            memcpy(original, data.values, sizeof(original));
            int result = max_window_sum(data.values, n, k);
            if (memcmp(original, data.values, sizeof(original)) != 0
                    || data.before != -987654321 || data.after != -987654321) fail();
            printf("MAX %d\n", result);
        } else return 2;
    }
    return 0;
}
