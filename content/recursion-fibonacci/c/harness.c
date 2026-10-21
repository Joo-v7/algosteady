#include "submission.c"

int main(void)
{
    int n;
    int scanned;
    while ((scanned = scanf("%d", &n)) == 1) {
        if (n < 0 || n > 30) return 2;
        printf("%d\n", fib(n));
    }
    return scanned == EOF ? 0 : 2;
}
