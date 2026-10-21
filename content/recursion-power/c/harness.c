#include "submission.c"

int main(void)
{
    double x;
    int n;
    int scanned;
    while ((scanned = scanf("%lf %d", &x, &n)) == 2) {
        if (x < -10 || x > 10 || n < 0 || n > 12) return 2;
        printf("%.6f\n", power(x, n));
    }
    return scanned == EOF ? 0 : 2;
}
