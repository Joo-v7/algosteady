#include <string.h>

#include "submission.c"

/* 각 테스트는 INIT으로 사용할 덱을 초기화한다. */
int main(void)
{
    DequeType deques[2] = {0};
    char command[16];
    int deque_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &deque_id) != 1 || deque_id < 0 || deque_id > 1) {
            fprintf(stderr, "호출 코드 입력 오류: 덱 번호\n");
            return 2;
        }
        DequeType *q = &deques[deque_id];

        if (strcmp(command, "INIT") == 0) {
            init_deque(q);
        } else if (strcmp(command, "ADD_FRONT") == 0 || strcmp(command, "ADD_REAR") == 0) {
            if (scanf("%d", &value) != 1) {
                fprintf(stderr, "호출 코드 입력 오류: 삽입 값\n");
                return 2;
            }
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
