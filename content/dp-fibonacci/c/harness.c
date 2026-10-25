#include <stdlib.h>
#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "피보나치 값·캐시 재사용·계산 범위·배열 경계 검사 오류\n");
    exit(2);
}

static int expected_fib(int n)
{
    if (n < 2) return n;
    int previous = 0;
    int current = 1;
    for (int i = 2; i <= n; i++) {
        int next = previous + current;
        previous = current;
        current = next;
    }
    return current;
}

static void reset(int memo[], int table[])
{
    for (int i = 0; i <= MAX_N; i++) memo[i] = table[i] = -1;
    memo[0] = 0;
    memo[1] = 1;
}

int main(void)
{
    struct { int before; int values[MAX_N + 1]; int after; } memo, table;
    memo.before = memo.after = table.before = table.after = -987654321;
    reset(memo.values, table.values);
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "RESET") == 0) {
            reset(memo.values, table.values);
            printf("RESET\n");
            continue;
        }
        int n;
        if (scanf("%d", &n) != 1 || n < 0 || n > MAX_N) return 2;
        if (strcmp(command, "SEED") == 0) {
            memo.values[n] = expected_fib(n);
        } else if (strcmp(command, "CACHE") == 0 || strcmp(command, "VALUES") == 0) {
            int *values = strcmp(command, "CACHE") == 0 ? memo.values : table.values;
            printf("%s", command);
            for (int i = 0; i <= n; i++) printf(" %d", values[i]);
            printf("\n");
        } else if (strcmp(command, "MEMO") == 0 || strcmp(command, "TABLE") == 0) {
            int original_memo[MAX_N + 1];
            int original_table[MAX_N + 1];
            memcpy(original_memo, memo.values, sizeof(original_memo));
            memcpy(original_table, table.values, sizeof(original_table));
            int result;
            if (strcmp(command, "MEMO") == 0) {
                result = fib_memo(n, memo.values);
                if (memcmp(original_table, table.values, sizeof(original_table)) != 0) fail();
                if (original_memo[n] != -1 && memcmp(original_memo, memo.values, sizeof(original_memo)) != 0) fail();
                for (int i = 0; i <= MAX_N; i++) {
                    if ((i > n || original_memo[i] != -1) && memo.values[i] != original_memo[i]) fail();
                    if (memo.values[i] != -1 && memo.values[i] != expected_fib(i)) fail();
                }
            } else {
                result = fib_table(n, table.values);
                if (memcmp(original_memo, memo.values, sizeof(original_memo)) != 0) fail();
                for (int i = 0; i <= MAX_N; i++) {
                    if (i <= n && table.values[i] != expected_fib(i)) fail();
                    if (i > n && table.values[i] != original_table[i]) fail();
                }
            }
            if (result != expected_fib(n)) fail();
            printf("%s %d\n", command, result);
        } else return 2;
        if (memo.before != -987654321 || memo.after != -987654321
                || table.before != -987654321 || table.after != -987654321) fail();
    }
    return 0;
}
