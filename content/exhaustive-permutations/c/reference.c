#include <stdio.h>

#define MAX_N 7

static void print_sequence(const int selected[], int length)
{
    printf("{");
    for (int i = 0; i < length; i++) {
        if (i > 0) printf(" ");
        printf("%d", selected[i]);
    }
    printf("}\n");
}

void generate_permutations(int n, int r, int depth, int selected[], int used[])
{
    if (depth == r) {
        print_sequence(selected, r);
        return;
    }
    for (int value = 1; value <= n; value++) {
        if (used[value]) continue;
        selected[depth] = value;
        used[value] = 1;
        generate_permutations(n, r, depth + 1, selected, used);
        used[value] = 0;
    }
}
