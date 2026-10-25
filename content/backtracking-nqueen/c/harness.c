#include <string.h>
#include "submission.c"

int main(void)
{
    char command[16];
    while (scanf("%15s", command) == 1) {
        int n;
        int row = 0;
        int column = 0;
        if (scanf("%d", &n) != 1 || n < 1 || n > MAX_N) return 2;
        if (strcmp(command, "CONTINUE") == 0) {
            if (scanf("%d", &row) != 1 || row < 0 || row > n) return 2;
        } else if (strcmp(command, "SAFE") == 0) {
            if (scanf("%d %d", &row, &column) != 2 || row < 0 || row >= n || column < 0 || column >= n) return 2;
        } else if (strcmp(command, "SOLVE") != 0) return 2;

        struct { int before; int columns[MAX_N]; int after; } state;
        state.before = state.after = -987654321;
        for (int i = 0; i < MAX_N; i++) state.columns[i] = -1;
        for (int i = 0; i < row; i++) {
            if (scanf("%d", &state.columns[i]) != 1 || state.columns[i] < 0 || state.columns[i] >= n) return 2;
            for (int previous = 0; previous < i; previous++) {
                int delta = state.columns[previous] - state.columns[i];
                if (delta == 0 || delta == i - previous || delta == previous - i) return 2;
            }
        }
        int original[MAX_N];
        memcpy(original, state.columns, sizeof(original));
        int result;
        if (strcmp(command, "SAFE") == 0) {
            result = is_safe(state.columns, row, column);
            if (result != 0 && result != 1) return 2;
            printf("SAFE %d\n", result);
        } else {
            result = solve_n_queens(n, row, state.columns);
            printf("COUNT %d\n", result);
        }
        if (state.before != -987654321 || state.after != -987654321
                || memcmp(original, state.columns, sizeof(original)) != 0) return 2;
    }
    return 0;
}
