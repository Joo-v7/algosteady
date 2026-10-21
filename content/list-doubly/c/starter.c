#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct DListNode {
    element data;
    struct DListNode *llink;
    struct DListNode *rlink;
} DListNode;

void init(DListNode *phead);
void dinsert(DListNode *before, element value);
void ddelete(DListNode *head, DListNode *removed);
void print_dlist(DListNode *phead);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
