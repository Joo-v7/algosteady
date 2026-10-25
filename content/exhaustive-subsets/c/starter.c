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

// 재귀 함수의 정의와 본문 구현
void generate_subsets(int n, int index, int selected[]);
