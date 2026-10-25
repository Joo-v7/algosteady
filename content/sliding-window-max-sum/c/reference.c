#include <stdio.h>

#define MAX_SIZE 100

int max_window_sum(const int values[], int n, int k)
{
    // 첫 K개 원소의 합으로 현재 합·최대 합 초기화
    int sum = 0;
    for (int i = 0; i < k; i++) {
        sum += values[i];
    }
    int best = sum;

    // 빠지는 원소를 빼고 들어오는 원소를 더해 구간 이동
    for (int i = k; i < n; i++) {
        sum = sum - values[i - k] + values[i];
        if (sum > best) {
            best = sum;
        }
    }
    return best;
}
