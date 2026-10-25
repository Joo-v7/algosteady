#include <stdio.h>

#define MAX_SIZE 100

int partition(int list[], int left, int right)
{
    int pivot = list[left];
    int low = left + 1;
    int high = right;

    while (1) {
        while (low <= right && list[low] < pivot) {
            low++;
        }
        while (high > left && list[high] > pivot) {
            high--;
        }
        if (low >= high) {
            break;
        }
        int temp = list[low];
        list[low] = list[high];
        list[high] = temp;
        low++;
        high--;
    }

    int temp = list[left];
    list[left] = list[high];
    list[high] = temp;
    return high;
}

void quick_sort(int list[], int left, int right)
{
    if (left < right) {
        int q = partition(list, left, right);
        quick_sort(list, left, q - 1);
        quick_sort(list, q + 1, right);
    }
}
