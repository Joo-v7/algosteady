#include <stdio.h>
#include <string.h>

#define MAX_LENGTH 100

int lcs_length(const char a[], const char b[], int table[][MAX_LENGTH + 1])
{
    int m = (int)strlen(a);
    int n = (int)strlen(b);

    // 빈 접두사의 기저 행·열 초기화
    for (int i = 0; i <= m; i++) {
        table[i][0] = 0;
    }
    for (int j = 0; j <= n; j++) {
        table[0][j] = 0;
    }

    // 같은 문자는 대각선에 추가, 다른 문자는 위·왼쪽 비교
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (a[i - 1] == b[j - 1]) {
                table[i][j] = table[i - 1][j - 1] + 1;
            } else {
                int best = table[i - 1][j];
                if (table[i][j - 1] > best) {
                    best = table[i][j - 1];
                }
                table[i][j] = best;
            }
        }
    }
    return table[m][n];
}
