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

/* top부터 NULL까지 노드 수와 종료 연결을 검사한다. */
static int stack_size(LinkedStackType *s)
{
    int count = 0;
    for (StackNode *node = s->top; node != NULL; node = node->link) {
        if (count >= allocation_count) {
            fprintf(stderr, "스택 연결 오류\n");
            exit(2);
        }
        count++;
    }
    return count;
}

/* 재초기화 전과 정상 종료 시 남은 노드를 정리한다. */
static void clear_stack(LinkedStackType *s)
{
    stack_size(s);
    StackNode *node = s->top;
    while (node != NULL) {
        StackNode *next = node->link;
        release_node(node);
        node = next;
    }
}

int main(void)
{
    LinkedStackType stacks[2] = {0};
    init(&stacks[0]);
    init(&stacks[1]);
    char command[16];
    int stack_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &stack_id) != 1 || stack_id < 0 || stack_id > 1) return 2;
        LinkedStackType *s = &stacks[stack_id];
        if (strcmp(command, "INIT") == 0) {
            clear_stack(s);
            init(s);
        } else if (strcmp(command, "PUSH") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            push(s, value);
        } else if (strcmp(command, "POP") == 0) {
            printf("POP %d\n", pop(s));
        } else if (strcmp(command, "PEEK") == 0) {
            printf("PEEK %d\n", peek(s));
        } else if (strcmp(command, "EMPTY") == 0) {
            printf("EMPTY %d\n", is_empty(s));
        } else if (strcmp(command, "FULL") == 0) {
            printf("FULL %d\n", is_full(s));
        } else if (strcmp(command, "PRINT") == 0) {
            print_stack(s);
        } else if (strcmp(command, "STATE") == 0) {
            int count = stack_size(s);
            StackNode *node = s->top;
            printf("STATE");
            for (int i = 0; i < count; i++) {
                printf(" %d", node->data);
                node = node->link;
            }
            printf("\n");
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", stack_size(s));
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            return 2;
        }
    }

    clear_stack(&stacks[0]);
    clear_stack(&stacks[1]);
    return 0;
}
