#include <stdio.h>
#include <stdlib.h>
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

/* 양 끝의 NULL과 모든 노드의 양방향 연결을 검사한다. */
static int deque_size(LinkedDequeType *q)
{
    DequeNode *previous = NULL;
    int count = 0;
    for (DequeNode *node = q->front; node != NULL; node = node->rlink) {
        if (count >= allocation_count || node->llink != previous) {
            fprintf(stderr, "덱 연결 오류\n");
            exit(2);
        }
        previous = node;
        count++;
    }
    if (q->rear != previous) {
        fprintf(stderr, "rear 연결 오류\n");
        exit(2);
    }
    return count;
}

/* 재초기화 전과 정상 종료 시 남은 노드를 정리한다. */
static void clear_deque(LinkedDequeType *q)
{
    deque_size(q);
    DequeNode *node = q->front;
    while (node != NULL) {
        DequeNode *next = node->rlink;
        release_node(node);
        node = next;
    }
}

int main(void)
{
    LinkedDequeType deques[2] = {0};
    init_deque(&deques[0]);
    init_deque(&deques[1]);
    char command[16];
    int deque_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &deque_id) != 1 || deque_id < 0 || deque_id > 1) return 2;
        LinkedDequeType *q = &deques[deque_id];
        if (strcmp(command, "INIT") == 0) {
            clear_deque(q);
            init_deque(q);
        } else if (strcmp(command, "ADD_FRONT") == 0 || strcmp(command, "ADD_REAR") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            if (strcmp(command, "ADD_FRONT") == 0) add_front(q, value);
            else add_rear(q, value);
        } else if (strcmp(command, "DELETE_FRONT") == 0) {
            printf("DELETE_FRONT %d\n", delete_front(q));
        } else if (strcmp(command, "DELETE_REAR") == 0) {
            printf("DELETE_REAR %d\n", delete_rear(q));
        } else if (strcmp(command, "GET_FRONT") == 0) {
            printf("GET_FRONT %d\n", get_front(q));
        } else if (strcmp(command, "GET_REAR") == 0) {
            printf("GET_REAR %d\n", get_rear(q));
        } else if (strcmp(command, "EMPTY") == 0) {
            printf("EMPTY %d\n", is_empty(q));
        } else if (strcmp(command, "FULL") == 0) {
            printf("FULL %d\n", is_full(q));
        } else if (strcmp(command, "STATE") == 0 || strcmp(command, "REVERSE") == 0) {
            int count = deque_size(q);
            int reverse = strcmp(command, "REVERSE") == 0;
            DequeNode *node = reverse ? q->rear : q->front;
            printf("%s", command);
            for (int i = 0; i < count; i++) {
                printf(" %d", node->data);
                node = reverse ? node->llink : node->rlink;
            }
            printf("\n");
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", deque_size(q));
        } else if (strcmp(command, "ENDS") == 0) {
            if (deque_size(q) == 0) printf("ENDS NULL NULL\n");
            else printf("ENDS %d %d\n", q->front->data, q->rear->data);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            return 2;
        }
    }

    clear_deque(&deques[0]);
    clear_deque(&deques[1]);
    return 0;
}
