#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

#define CHECK_CAPACITY 100

static void fail(void)
{
    fprintf(stderr, "정렬 결과·범위·원소 보존 검사 오류\n");
    exit(2);
}

static int compare(const void *left, const void *right)
{
    int a = *(const int *)left;
    int b = *(const int *)right;
    return (a > b) - (a < b);
}

int main(void)
{
    int storage[CHECK_CAPACITY + 2];
    int *values = storage + 1;
    int n = -1;
    char command[16];

    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "LOAD") == 0) {
            if (scanf("%d", &n) != 1 || n < 0 || n > CHECK_CAPACITY) return 2;
            for (int i = 0; i < CHECK_CAPACITY + 2; i++) storage[i] = 123456789;
            for (int i = 0; i < n; i++) {
                if (scanf("%d", &values[i]) != 1 || values[i] < -1000000 || values[i] > 1000000) return 2;
            }
            continue;
        }
        if (n < 0) return 2;
        int original[CHECK_CAPACITY + 2];
        memcpy(original, storage, sizeof(storage));
        int left = 0;
        int right = n - 1;
        int helper = 0;

        if (strcmp(command, "SORT") == 0) {
            if (scanf("%d %d", &left, &right) != 2 || left < 0 || left > n || right < -1 || right >= n) return 2;
            merge_sort(values, left, right);
        } else if (strcmp(command, "MERGE") == 0) {
            int mid;
            if (scanf("%d %d %d", &left, &mid, &right) != 3 || left < 0 || left > mid || mid >= right || right >= n) return 2;
            for (int i = left + 1; i <= right; i++) {
                if (i != mid + 1 && values[i - 1] > values[i]) return 2;
            }
            merge(values, left, mid, right);
            helper = 1;
        } else return 2;

        // 정렬 대상 밖과 배열 경계의 값 보존 확인
        for (int i = -1; i <= CHECK_CAPACITY; i++) {
            if ((i < left || i > right) && values[i] != original[i + 1]) fail();
        }
        if (helper) {
            qsort(original + left + 1, (size_t)(right - left + 1), sizeof(int), compare);
            if (memcmp(values + left, original + left + 1, (size_t)(right - left + 1) * sizeof(int)) != 0) fail();
            printf("MERGE OK\n");
        } else {
            printf("SORT");
            for (int i = 0; i < n; i++) printf(" %d", values[i]);
            printf("\n");
        }
    }
    return 0;
}
