#include <stdio.h>

#define MAX_SIZE 100

typedef struct {
    int left;
    int right;
} Pair;

Pair two_sum(const int values[], int n, int target)
{
    int left = 0;
    int right = n - 1;

    // 양 끝의 합에 따라 한쪽 포인터 이동
    while (left < right) {
        int sum = values[left] + values[right];
        if (sum == target) {
            return (Pair){left, right};
        }
        if (sum < target) {
            left++;
        } else {
            right--;
        }
    }
    return (Pair){-1, -1};
}
