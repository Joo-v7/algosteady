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

/* front부터 NULL까지의 연결과 마지막 노드인 rear를 검사한다. */
static int queue_size(LinkedQueueType *q)
{
    QueueNode *last = NULL;
    int count = 0;
    for (QueueNode *node = q->front; node != NULL; node = node->link) {
        if (count >= allocation_count) {
            fprintf(stderr, "큐 연결 오류\n");
            exit(2);
        }
        last = node;
        count++;
    }
    if (q->rear != last) {
        fprintf(stderr, "rear 연결 오류\n");
        exit(2);
    }
    return count;
}

/* 재초기화 전과 정상 종료 시 남은 노드를 정리한다. */
static void clear_queue(LinkedQueueType *q)
{
    queue_size(q);
    QueueNode *node = q->front;
    while (node != NULL) {
        QueueNode *next = node->link;
        release_node(node);
        node = next;
    }
}

int main(void)
{
    LinkedQueueType queues[2] = {0};
    init(&queues[0]);
    init(&queues[1]);
    char command[16];
    int queue_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &queue_id) != 1 || queue_id < 0 || queue_id > 1) return 2;
        LinkedQueueType *q = &queues[queue_id];
        if (strcmp(command, "INIT") == 0) {
            clear_queue(q);
            init(q);
        } else if (strcmp(command, "ENQUEUE") == 0) {
            if (scanf("%d", &value) != 1) return 2;
            enqueue(q, value);
        } else if (strcmp(command, "DEQUEUE") == 0) {
            printf("DEQUEUE %d\n", dequeue(q));
        } else if (strcmp(command, "EMPTY") == 0) {
            printf("EMPTY %d\n", is_empty(q));
        } else if (strcmp(command, "FULL") == 0) {
            printf("FULL %d\n", is_full(q));
        } else if (strcmp(command, "PRINT") == 0) {
            print_queue(q);
        } else if (strcmp(command, "STATE") == 0) {
            int count = queue_size(q);
            QueueNode *node = q->front;
            printf("STATE");
            for (int i = 0; i < count; i++) {
                printf(" %d", node->data);
                node = node->link;
            }
            printf("\n");
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", queue_size(q));
        } else if (strcmp(command, "ENDS") == 0) {
            if (queue_size(q) == 0) printf("ENDS NULL NULL\n");
            else printf("ENDS %d %d\n", q->front->data, q->rear->data);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            return 2;
        }
    }

    clear_queue(&queues[0]);
    clear_queue(&queues[1]);
    return 0;
}
