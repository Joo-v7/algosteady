#include <stdio.h>
#include <stdlib.h>

#define MAX_N 10

int is_safe(const int columns[], int row, int column)
{
    for (int previous = 0; previous < row; previous++) {
        if (columns[previous] == column
                || abs(columns[previous] - column) == row - previous) {
            return 0;
        }
    }
    return 1;
}

int solve_n_queens(int n, int row, int columns[])
{
    if (row == n) return 1;
    int count = 0;
    for (int column = 0; column < n; column++) {
        if (!is_safe(columns, row, column)) continue;
        columns[row] = column;
        count += solve_n_queens(n, row + 1, columns);
        columns[row] = -1;
    }
    return count;
}
