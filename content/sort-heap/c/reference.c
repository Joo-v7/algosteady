#include <stdio.h>

#define MAX_SIZE 100

void sift_down(int list[], int root, int n)
{
    int item = list[root];
    int child = root * 2 + 1;
    while (child < n) {
        if (child + 1 < n && list[child] < list[child + 1]) {
            child++;
        }
        if (item >= list[child]) {
            break;
        }
        list[root] = list[child];
        root = child;
        child = root * 2 + 1;
    }
    list[root] = item;
}

void heap_sort(int list[], int n)
{
    // 마지막 부모부터 최대 힙 구성
    for (int root = n / 2 - 1; root >= 0; root--) {
        sift_down(list, root, n);
    }

    // 최댓값을 뒤로 이동하고 남은 힙 복원
    for (int end = n - 1; end > 0; end--) {
        int temp = list[0];
        list[0] = list[end];
        list[end] = temp;
        sift_down(list, 0, end);
    }
}
