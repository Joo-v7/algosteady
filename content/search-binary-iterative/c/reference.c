#include <stdio.h>

#define MAX_SIZE 100

int list[MAX_SIZE];

int search_binary(int key, int low, int high)
{
    while (low <= high) {
        int middle = (low + high) / 2;
        if (key == list[middle]) return middle;
        if (key > list[middle]) low = middle + 1;
        else high = middle - 1;
    }
    return -1;
}
