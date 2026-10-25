#include <stdio.h>

#define MAX_SIZE 100

int sorted[MAX_SIZE];

void merge(int list[], int left, int mid, int right)
{
    int i = left;
    int j = mid + 1;
    int k = left;

    // 두 정렬 구간의 앞쪽 값 비교
    while (i <= mid && j <= right) {
        if (list[i] <= list[j]) {
            sorted[k++] = list[i++];
        } else {
            sorted[k++] = list[j++];
        }
    }
    while (i <= mid) {
        sorted[k++] = list[i++];
    }
    while (j <= right) {
        sorted[k++] = list[j++];
    }

    // 병합한 구간만 원본 배열에 반영
    for (int index = left; index <= right; index++) {
        list[index] = sorted[index];
    }
}

void merge_sort(int list[], int left, int right)
{
    if (left < right) {
        int mid = left + (right - left) / 2;
        merge_sort(list, left, mid);
        merge_sort(list, mid + 1, right);
        merge(list, left, mid, right);
    }
}
