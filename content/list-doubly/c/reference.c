#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct DListNode {
    element data;
    struct DListNode *llink;
    struct DListNode *rlink;
} DListNode;

void init(DListNode *phead)
{
    phead->llink = phead;
    phead->rlink = phead;
}

void dinsert(DListNode *before, element value)
{
    DListNode *node = malloc(sizeof(DListNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = value;
    node->llink = before;
    node->rlink = before->rlink;
    before->rlink->llink = node;
    before->rlink = node;
}

void ddelete(DListNode *head, DListNode *removed)
{
    if (removed == head) return;
    removed->llink->rlink = removed->rlink;
    removed->rlink->llink = removed->llink;
    free(removed);
}

void print_dlist(DListNode *phead)
{
    for (DListNode *p = phead->rlink; p != phead; p = p->rlink) {
        printf("<-| %d |-> ", p->data);
    }
    printf("\n");
}
