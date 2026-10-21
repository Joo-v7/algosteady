#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct ListNode {
    element data;
    struct ListNode *link;
} ListNode;

ListNode *insert_first(ListNode *head, element value)
{
    ListNode *node = malloc(sizeof(ListNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = value;
    if (head == NULL) {
        head = node;
        node->link = head;
    } else {
        node->link = head->link;
        head->link = node;
    }
    return head;
}

ListNode *insert_last(ListNode *head, element value)
{
    ListNode *node = malloc(sizeof(ListNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->data = value;
    if (head == NULL) {
        head = node;
        node->link = head;
    } else {
        node->link = head->link;
        head->link = node;
        head = node;
    }
    return head;
}

void print_list(ListNode *head)
{
    if (head == NULL) return;
    ListNode *first = head->link;
    ListNode *p = first;
    do {
        printf("%d->", p->data);
        p = p->link;
    } while (p != first);
    printf("\n");
}
