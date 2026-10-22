#include <stdio.h>
#include <stdlib.h>

#define MAX_ELEMENT 200

typedef struct {
    int key;
} element;

typedef struct {
    element heap[MAX_ELEMENT];
    int heap_size;
} HeapType;

void init(HeapType *h);
void insert_min_heap(HeapType *h, element item);
element delete_min_heap(HeapType *h);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
