#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "힙 크기·순서·값 보존 오류\n");
    exit(2);
}

/* 힙 순서와 값 보존을 독립적으로 검사한다. */
static void validate(HeapType *h, int *expected, int count)
{
    if (h->heap_size != count || count < 0 || count >= MAX_ELEMENT) fail();
    int used[MAX_ELEMENT] = {0};
    for (int i = 1; i <= count; i++) {
        if (i > 1 && h->heap[i / 2].key < h->heap[i].key) fail();
        int found = 0;
        for (int j = 0; j < count; j++) {
            if (!used[j] && expected[j] == h->heap[i].key) {
                used[j] = 1;
                found = 1;
                break;
            }
        }
        if (!found) fail();
    }
}

int main(void)
{
    HeapType heaps[2];
    int values[2][MAX_ELEMENT] = {{0}};
    int counts[2] = {0};
    for (int i = 0; i < 2; i++) {
        memset(&heaps[i], 0x55, sizeof(HeapType));
        init(&heaps[i]);
        validate(&heaps[i], values[i], 0);
    }
    char command[16];
    int id;
    int key;
    while (scanf("%15s %d", command, &id) == 2) {
        if (id < 0 || id > 1) return 2;
        if (strcmp(command, "INIT") == 0) {
            init(&heaps[id]);
            counts[id] = 0;
        } else if (strcmp(command, "PUSH") == 0) {
            if (scanf("%d", &key) != 1 || key < -1000000 || key > 1000000) return 2;
            element item = {key};
            insert_max_heap(&heaps[id], item);
            if (counts[id] == MAX_ELEMENT - 1) fail();
            values[id][counts[id]++] = key;
        } else if (strcmp(command, "POP") == 0) {
            element actual = delete_max_heap(&heaps[id]);
            if (counts[id] == 0) fail();
            int best = 0;
            for (int i = 1; i < counts[id]; i++) {
                if (values[id][i] > values[id][best]) best = i;
            }
            if (actual.key != values[id][best]) fail();
            values[id][best] = values[id][--counts[id]];
            printf("POP %d\n", actual.key);
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", heaps[id].heap_size);
        } else if (strcmp(command, "ROOT") == 0) {
            if (heaps[id].heap_size == 0) printf("ROOT EMPTY\n");
            else printf("ROOT %d\n", heaps[id].heap[1].key);
        } else return 2;
        for (int i = 0; i < 2; i++) validate(&heaps[i], values[i], counts[i]);
    }
    return 0;
}
