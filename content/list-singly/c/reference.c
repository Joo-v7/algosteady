#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct ListNode {
    element data;
    struct ListNode *link;
} ListNode;

ListNode *insert_first(ListNode *head, element value)
{
    ListNode *p = malloc(sizeof(ListNode));
    if (p == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    p->data = value;
    p->link = head;
    return p;
}

ListNode *insert(ListNode *head, ListNode *pre, element value)
{
    ListNode *p = malloc(sizeof(ListNode));
    if (p == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    p->data = value;
    p->link = pre->link;
    pre->link = p;
    return head;
}

ListNode *delete_first(ListNode *head)
{
    if (head == NULL) return NULL;
    ListNode *removed = head;
    head = removed->link;
    free(removed);
    return head;
}

ListNode *delete(ListNode *head, ListNode *pre)
{
    ListNode *removed = pre->link;
    pre->link = removed->link;
    free(removed);
    return head;
}

void print_list(ListNode *head)
{
    for (ListNode *p = head; p != NULL; p = p->link) {
        printf("%d->", p->data);
    }
    printf("NULL\n");
}
