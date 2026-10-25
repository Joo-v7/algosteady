#include <string.h>
#include "submission.c"

int main(void)
{
    char command[16];
    while (scanf("%15s", command) == 1) {
        int n;
        int r;
        if (strcmp(command, "GENERATE") != 0 || scanf("%d %d", &n, &r) != 2
                || n < 0 || n > MAX_N || r < 0 || r > n) return 2;
        int guarded[MAX_N + 2];
        for (int i = 0; i < MAX_N + 2; i++) guarded[i] = -987654321;
        int *selected = &guarded[1];
        struct { int before; int values[MAX_N + 1]; int after; } state = {-987654321, {0}, -987654321};
        generate_permutations(n, r, 0, selected, state.values);
        for (int i = 0; i < MAX_N + 2; i++) {
            if (i >= 1 && i <= r) continue;
            if (guarded[i] != -987654321) return 2;
        }
        if (state.before != -987654321 || state.after != -987654321) return 2;
        for (int value = 0; value <= MAX_N; value++) {
            if (state.values[value] != 0) return 2;
        }
    }
    return 0;
}
