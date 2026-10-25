#include <stdio.h>

#define MAX_N 46

int fib_memo(int n, int memo[])
{
    if (memo[n] != -1) return memo[n];
    int previous = fib_memo(n - 1, memo);
    int before_previous = fib_memo(n - 2, memo);
    memo[n] = previous + before_previous;
    return memo[n];
}

int fib_table(int n, int table[])
{
    table[0] = 0;
    if (n >= 1) table[1] = 1;
    for (int i = 2; i <= n; i++) {
        table[i] = table[i - 1] + table[i - 2];
    }
    return table[n];
}
