#include <stdio.h>

#define MAX_ITEMS 20
#define MAX_CAPACITY 100

typedef struct {
    int weight;
    int value;
} Item;

int knapsack_01(const Item items[], int n, int capacity, int table[][MAX_CAPACITY + 1])
{
    // 물건이 없는 기저 행 초기화
    for (int w = 0; w <= capacity; w++) {
        table[0][w] = 0;
    }

    // 이전 행에서 제외·포함한 가치를 비교해 현재 행 저장
    for (int i = 1; i <= n; i++) {
        for (int w = 0; w <= capacity; w++) {
            int best = table[i - 1][w];
            if (items[i - 1].weight <= w) {
                int included = table[i - 1][w - items[i - 1].weight] + items[i - 1].value;
                if (included > best) {
                    best = included;
                }
            }
            table[i][w] = best;
        }
    }
    return table[n][capacity];
}
