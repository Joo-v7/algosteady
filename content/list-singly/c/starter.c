#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct ListNode {
    element data;
    struct ListNode *link;
} ListNode;

ListNode *insert_first(ListNode *head, element value);
ListNode *insert(ListNode *head, ListNode *pre, element value);
ListNode *delete_first(ListNode *head);
ListNode *delete(ListNode *head, ListNode *pre);
void print_list(ListNode *head);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
