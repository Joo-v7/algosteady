#include "submission.c"

int main(void)
{
    int n;
    char from, tmp, to;
    int scanned;
    while ((scanned = scanf("%d %c %c %c", &n, &from, &tmp, &to)) == 4) {
        if (n < 1 || n > 8 || from < 'A' || from > 'C' || tmp < 'A' || tmp > 'C'
                || to < 'A' || to > 'C' || from == tmp || from == to || tmp == to) return 2;
        hanoi_tower(n, from, tmp, to);
    }
    return scanned == EOF ? 0 : 2;
}
