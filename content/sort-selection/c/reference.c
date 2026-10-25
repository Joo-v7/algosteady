#include <stdio.h>

#define MAX_SIZE 100

void selection_sort(int list[], int n)
{
    for (int i = 0; i < n - 1; i++) {
        int min = i;
        for (int j = i + 1; j < n; j++) {
            if (list[j] < list[min]) {
                min = j;
            }
        }
        int temp = list[i];
        list[i] = list[min];
        list[min] = temp;
    }
}
