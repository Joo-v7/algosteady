#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int allocation_count;
static int release_count;

/* 사용자 코드의 데이터 노드 할당·해제 호출 수를 검사한다. */
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

/* 헤드 노드로 닫히는 고리와 양방향 링크의 일치를 검사한다. */
static int list_size(DListNode *head)
{
    DListNode *previous = head;
    DListNode *node = head->rlink;
    int count = 0;
    while (node != head) {
        if (node == NULL || count >= allocation_count || node->llink != previous) {
            fprintf(stderr, "양방향 연결 오류\n");
            exit(2);
        }
        previous = node;
        node = node->rlink;
        count++;
    }
    if (head->llink != previous) {
        fprintf(stderr, "헤드 노드 연결 오류\n");
        exit(2);
    }
    return count;
}

/* 헤드 노드를 제외한 나머지 노드는 호출 코드가 정리한다. */
static void clear_list(DListNode *head)
{
    list_size(head);
    DListNode *node = head->rlink;
    while (node != head) {
        DListNode *next = node->rlink;
        release_node(node);
        node = next;
    }
    head->llink = head;
    head->rlink = head;
}

/* -1은 헤드 노드, 0부터는 정방향 데이터 노드의 위치이다. */
static DListNode *node_at(DListNode *head, int index)
{
    int count = list_size(head);
    if (index < -1 || index >= count) exit(2);
    DListNode *node = head;
    for (int i = -1; i < index; i++) node = node->rlink;
    return node;
}

int main(void)
{
    DListNode heads[2] = {0};
    init(&heads[0]);
    init(&heads[1]);
    char command[16];
    int list_id;
    int index;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &list_id) != 1 || list_id < 0 || list_id > 1) return 2;
        DListNode *head = &heads[list_id];
        if (strcmp(command, "INIT") == 0) {
            clear_list(head);
            init(head);
        } else if (strcmp(command, "INSERT") == 0) {
            if (scanf("%d %d", &index, &value) != 2) return 2;
            dinsert(node_at(head, index), value);
        } else if (strcmp(command, "DELETE") == 0) {
            if (scanf("%d", &index) != 1) return 2;
            ddelete(head, node_at(head, index));
        } else if (strcmp(command, "PRINT") == 0) {
            print_dlist(head);
        } else if (strcmp(command, "STATE") == 0 || strcmp(command, "REVERSE") == 0) {
            int count = list_size(head);
            int reverse = strcmp(command, "REVERSE") == 0;
            DListNode *node = reverse ? head->llink : head->rlink;
            printf("%s", command);
            for (int i = 0; i < count; i++) {
                printf(" %d", node->data);
                node = reverse ? node->llink : node->rlink;
            }
            printf("\n");
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", list_size(head));
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            return 2;
        }
    }

    clear_list(&heads[0]);
    clear_list(&heads[1]);
    return 0;
}
