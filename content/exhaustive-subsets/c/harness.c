#include <string.h>
#include "submission.c"

int main(void)
{
    char command[16];
    while (scanf("%15s", command) == 1) {
        int n;
        if (strcmp(command, "GENERATE") != 0 || scanf("%d", &n) != 1 || n < 0 || n > MAX_N) return 2;
        struct { int before; int values[MAX_N + 1]; int after; } state = {-987654321, {0}, -987654321};
        generate_subsets(n, 1, state.values);
        if (state.before != -987654321 || state.after != -987654321) return 2;
        for (int value = 0; value <= MAX_N; value++) {
            if (state.values[value] != 0) return 2;
        }
    }
    return 0;
}
