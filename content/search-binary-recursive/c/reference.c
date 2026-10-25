#include <stdio.h>

#define MAX_SIZE 100

int list[MAX_SIZE];

int search_binary(int key, int low, int high)
{
    if (low > high) return -1;
    int middle = (low + high) / 2;
    if (key == list[middle]) return middle;
    if (key < list[middle]) return search_binary(key, low, middle - 1);
    return search_binary(key, middle + 1, high);
}
