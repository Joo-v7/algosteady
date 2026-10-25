#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "입력 물건·표 계산 범위·배열 경계 검사 오류\n");
    exit(2);
}

int main(void)
{
    struct { int before; Item values[MAX_ITEMS]; int after; } items;
    struct { int before; int values[MAX_ITEMS + 1][MAX_CAPACITY + 1]; int after; } table;
    items.before = items.after = table.before = table.after = -987654321;
    int n = -1;
    char command[16];
    while (scanf("%15s", command) == 1) {
        int capacity;
        if (strcmp(command, "SOLVE") == 0) {
            if (scanf("%d%d", &n, &capacity) != 2 || n < 0 || n > MAX_ITEMS) return 2;
            memset(items.values, 0, sizeof(items.values));
            for (int i = 0; i < n; i++) {
                if (scanf("%d%d", &items.values[i].weight, &items.values[i].value) != 2
                        || items.values[i].weight < 1 || items.values[i].weight > MAX_CAPACITY
                        || items.values[i].value < 0 || items.values[i].value > 1000) return 2;
            }
            for (int i = 0; i <= MAX_ITEMS; i++) {
                for (int w = 0; w <= MAX_CAPACITY; w++) table.values[i][w] = -987654321;
            }
        } else if (strcmp(command, "AGAIN") == 0) {
            if (n < 0 || scanf("%d", &capacity) != 1) return 2;
        } else return 2;
        if (capacity < 0 || capacity > MAX_CAPACITY) return 2;

        // 호출 전 입력과 표의 계산 범위 밖 값 보관
        Item original_items[MAX_ITEMS];
        int original_table[MAX_ITEMS + 1][MAX_CAPACITY + 1];
        memcpy(original_items, items.values, sizeof(original_items));
        memcpy(original_table, table.values, sizeof(original_table));
        int result = knapsack_01(items.values, n, capacity, table.values);
        if (memcmp(original_items, items.values, sizeof(original_items)) != 0) fail();
        if (items.before != -987654321 || items.after != -987654321
                || table.before != -987654321 || table.after != -987654321) fail();
        for (int i = 0; i <= MAX_ITEMS; i++) {
            for (int w = 0; w <= MAX_CAPACITY; w++) {
                if ((i > n || w > capacity) && table.values[i][w] != original_table[i][w]) fail();
            }
        }

        printf("VALUE %d\nTABLE\n", result);
        for (int i = 0; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                if (w > 0) printf(" ");
                printf("%d", table.values[i][w]);
            }
            printf("\n");
        }
    }
    return 0;
}
