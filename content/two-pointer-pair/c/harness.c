#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "두 원소의 합·인덱스·배열 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    struct { int before; int values[MAX_SIZE]; int after; } data;
    data.before = data.after = -987654321;
    memset(data.values, 0x55, sizeof(data.values));
    int n = 0;
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "LOAD") == 0) {
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_SIZE) return 2;
            memset(data.values, 0x55, sizeof(data.values));
            for (int i = 0; i < n; i++) {
                if (scanf("%d", &data.values[i]) != 1 || data.values[i] < -1000000 || data.values[i] > 1000000) return 2;
                if (i > 0 && data.values[i] < data.values[i - 1]) return 2;
            }
        } else if (strcmp(command, "PAIR") == 0 || strcmp(command, "CHECK") == 0) {
            int target;
            if (scanf("%d", &target) != 1 || target < -2000000 || target > 2000000) return 2;

            // 모든 서로 다른 인덱스 쌍으로 존재 여부 확인
            int exists = 0;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (data.values[i] + data.values[j] == target) exists = 1;
                }
            }
            int original[MAX_SIZE];
            memcpy(original, data.values, sizeof(original));
            Pair result = two_sum(data.values, n, target);
            if (memcmp(original, data.values, sizeof(original)) != 0
                    || data.before != -987654321 || data.after != -987654321) fail();
            if (exists) {
                if (result.left < 0 || result.left >= result.right || result.right >= n
                        || data.values[result.left] + data.values[result.right] != target) fail();
            } else if (result.left != -1 || result.right != -1) fail();

            if (strcmp(command, "PAIR") == 0) {
                printf("PAIR %d %d\n", result.left, result.right);
            } else {
                printf("%s\n", exists ? "FOUND" : "ABSENT");
            }
        } else return 2;
    }
    return 0;
}
