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

/* 호출 코드가 선행 노드와 상태 검사 위치를 찾는다. */
static ListNode *node_at(ListNode *head, int index)
{
    if (index < 0) return NULL;
    for (int i = 0; i < index && head != NULL; i++) head = head->link;
    return head;
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
        if (scanf("%d", &list_id) != 1 || list_id < 0 || list_id > 1) {
            fprintf(stderr, "호출 코드 입력 오류: 리스트 번호\n");
            return 2;
        }
        ListNode *head = heads[list_id];
        if (strcmp(command, "INSERT_FIRST") == 0 || strcmp(command, "INSERT_AFTER") == 0
                || strcmp(command, "DELETE_FIRST") == 0 || strcmp(command, "DELETE_AFTER") == 0) {
            previous_heads[list_id] = (uintptr_t)head;
        }

        if (strcmp(command, "INIT") == 0) {
            while (head != NULL) {
                ListNode *next = head->link;
                release_node(head);
                head = next;
            }
            heads[list_id] = NULL;
        } else if (strcmp(command, "INSERT_FIRST") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            heads[list_id] = insert_first(head, value);
        } else if (strcmp(command, "INSERT_AFTER") == 0) {
            if (scanf("%d %d", &index, &value) != 2) return 2;
            ListNode *pre = node_at(head, index);
            if (pre == NULL) return 2;
            heads[list_id] = insert(head, pre, value);
        } else if (strcmp(command, "DELETE_FIRST") == 0) {
            heads[list_id] = delete_first(head);
        } else if (strcmp(command, "DELETE_AFTER") == 0) {
            if (scanf("%d", &index) != 1) return 2;
            ListNode *pre = node_at(head, index);
            if (pre == NULL || pre->link == NULL) return 2;
            heads[list_id] = delete(head, pre);
        } else if (strcmp(command, "PRINT") == 0) {
            print_list(head);
        } else if (strcmp(command, "STATE") == 0) {
            printf("STATE");
            for (ListNode *p = head; p != NULL; p = p->link) printf(" %d", p->data);
            printf("\n");
        } else if (strcmp(command, "NODE") == 0) {
            if (scanf("%d", &index) != 1) return 2;
            ListNode *node = node_at(head, index);
            if (node == NULL) return 2;
            printf("NODE %d %d\n", index, node->data);
        } else if (strcmp(command, "HEAD_SAME") == 0) {
            printf("HEAD_SAME %d\n", (uintptr_t)head == previous_heads[list_id]);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            fprintf(stderr, "호출 코드 입력 오류: 명령\n");
            return 2;
        }
    }

    for (int i = 0; i < 2; i++) {
        ListNode *head = heads[i];
        while (head != NULL) {
            ListNode *next = head->link;
            release_node(head);
            head = next;
        }
    }
    return 0;
}
