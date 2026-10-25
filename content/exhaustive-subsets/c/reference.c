#include <stdio.h>

#define MAX_N 10

static void print_sequence(const int selected[], int length)
{
    printf("{");
    for (int i = 0; i < length; i++) {
        if (i > 0) printf(" ");
        printf("%d", selected[i]);
    }
    printf("}\n");
}

void generate_subsets(int n, int index, int selected[])
{
    if (index > n) {
        int values[MAX_N];
        int count = 0;
        for (int value = 1; value <= n; value++) {
            if (selected[value]) values[count++] = value;
        }
        print_sequence(values, count);
        return;
    }

    // 현재 원소 제외 → 포함 → 선택 상태 복구
    selected[index] = 0;
    generate_subsets(n, index + 1, selected);
    selected[index] = 1;
    generate_subsets(n, index + 1, selected);
    selected[index] = 0;
}
