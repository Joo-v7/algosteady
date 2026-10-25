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

void generate_combinations(int n, int r, int start, int depth, int selected[])
{
    if (depth == r) {
        print_sequence(selected, r);
        return;
    }
    for (int value = start; value <= n; value++) {
        selected[depth] = value;
        generate_combinations(n, r, value + 1, depth + 1, selected);
    }
}
