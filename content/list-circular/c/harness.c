#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

static int allocation_count;
static int release_count;

/* 사용자 코드의 노드 할당·해제 호출 수를 검사한다. */
static void *allocate_node(size_t size)
{
    void *node = malloc(size);
    if (node != NULL) allocation_count++;
    return node;
}

static void release_node(void *node)
{
    if (node != NULL) release_count++;
    free(node);
}

#define malloc allocate_node
#define free release_node
#include "submission.c"
#undef malloc
#undef free

/* 닫힌 고리와 head가 실제 고리에 속하는지 검사한다. */
static int list_size(ListNode *head)
{
    if (head == NULL) return 0;
    ListNode *first = head->link;
    ListNode *node = first;
    int count = 0;
    int contains_head = 0;
    do {
        if (node == NULL || count >= allocation_count) {
            fprintf(stderr, "원형 연결 오류\n");
            exit(2);
        }
        if (node == head) contains_head = 1;
        count++;
        node = node->link;
    } while (node != first);
    if (!contains_head) {
        fprintf(stderr, "head 연결 오류\n");
        exit(2);
    }
    return count;
}

/* 순환을 끊은 뒤 호출 코드가 남은 노드를 해제한다. */
static void clear_list(ListNode *head)
{
    if (head == NULL) return;
    list_size(head);
    ListNode *node = head->link;
    head->link = NULL;
    while (node != NULL) {
        ListNode *next = node->link;
        release_node(node);
        node = next;
    }
}

int main(void)
{
    ListNode *heads[2] = {NULL, NULL};
    uintptr_t previous_heads[2] = {0, 0};
    char command[16];
    int list_id;
    int value;
    int index;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &list_id) != 1 || list_id < 0 || list_id > 1) return 2;
        ListNode *head = heads[list_id];
        if (strcmp(command, "INSERT_FIRST") == 0 || strcmp(command, "INSERT_LAST") == 0) {
            previous_heads[list_id] = (uintptr_t)head;
        }

        if (strcmp(command, "INIT") == 0) {
            clear_list(head);
            heads[list_id] = NULL;
        } else if (strcmp(command, "INSERT_FIRST") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            heads[list_id] = insert_first(head, value);
        } else if (strcmp(command, "INSERT_LAST") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            heads[list_id] = insert_last(head, value);
        } else if (strcmp(command, "PRINT") == 0) {
            print_list(head);
        } else if (strcmp(command, "STATE") == 0) {
            int count = list_size(head);
            ListNode *node = head == NULL ? NULL : head->link;
            printf("STATE");
            for (int i = 0; i < count; i++) {
                printf(" %d", node->data);
                node = node->link;
            }
            printf("\n");
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", list_size(head));
        } else if (strcmp(command, "HEAD") == 0 || strcmp(command, "FIRST") == 0) {
            if (list_size(head) == 0) return 2;
            printf("%s %d\n", command, strcmp(command, "HEAD") == 0 ? head->data : head->link->data);
        } else if (strcmp(command, "WALK") == 0) {
            if (scanf("%d", &index) != 1 || index < 0 || index > 1000) return 2;
            int count = list_size(head);
            if (count == 0 && index > 0) return 2;
            ListNode *node = head == NULL ? NULL : head->link;
            printf("WALK");
            for (int i = 0; i < index; i++) {
                printf(" %d", node->data);
                node = node->link;
            }
            printf("\n");
        } else if (strcmp(command, "HEAD_SAME") == 0) {
            printf("HEAD_SAME %d\n", (uintptr_t)head == previous_heads[list_id]);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            return 2;
        }
    }

    clear_list(heads[0]);
    clear_list(heads[1]);
    return 0;
}
