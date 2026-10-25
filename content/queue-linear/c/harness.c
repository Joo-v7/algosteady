#include <string.h>

#include "submission.c"

/* 각 테스트는 INIT으로 사용할 큐를 초기화한다. */
int main(void)
{
    QueueType queues[2] = {0};
    char command[16];
    int queue_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &queue_id) != 1 || queue_id < 0 || queue_id > 1) {
            fprintf(stderr, "호출 코드 입력 오류: 큐 번호\n");
            return 2;
        }

        QueueType *q = &queues[queue_id];

        if (strcmp(command, "INIT") == 0) {
            init_queue(q);
        } else if (strcmp(command, "ENQUEUE") == 0) {
            if (scanf("%d", &value) != 1) {
                fprintf(stderr, "호출 코드 입력 오류: 삽입 값\n");
                return 2;
            }
            enqueue(q, value);
        } else if (strcmp(command, "DEQUEUE") == 0) {
            printf("DEQUEUE %d\n", dequeue(q));
        } else if (strcmp(command, "EMPTY") == 0) {
            printf("EMPTY %d\n", is_empty(q));
        } else if (strcmp(command, "FULL") == 0) {
            printf("FULL %d\n", is_full(q));
        } else if (strcmp(command, "FRONT") == 0) {
            printf("FRONT %d\n", q->front);
        } else if (strcmp(command, "REAR") == 0) {
            printf("REAR %d\n", q->rear);
        } else if (strcmp(command, "DATA") == 0) {
            if (scanf("%d", &value) != 1 || value < 0 || value >= MAX_QUEUE_SIZE) {
                fprintf(stderr, "호출 코드 입력 오류: 배열 위치\n");
                return 2;
            }
            printf("DATA %d %d\n", value, q->data[value]);
        } else {
            fprintf(stderr, "호출 코드 입력 오류: 명령\n");
            return 2;
        }
    }
    return 0;
}
