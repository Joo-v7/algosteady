#include <stdio.h>

#define MAX_ITEMS 20
#define MAX_CAPACITY 100

typedef struct {
    int weight;
    int value;
} Item;

// 선언 아래에 함수 전체 구현
int knapsack_01(const Item items[], int n, int capacity, int table[][MAX_CAPACITY + 1]);
