#include <string.h>

#include "submission.c"

/* 재초기화 전과 정상 종료 시 할당된 배열을 해제한다. */
int main(void)
{
    StackType stacks[2] = {0};
    char command[16];
    int stack_id;
    int value;

    while (scanf("%15s", command) == 1) {
        if (scanf("%d", &stack_id) != 1 || stack_id < 0 || stack_id > 1) {
            fprintf(stderr, "호출 코드 입력 오류: 스택 번호\n");
            return 2;
        }

        StackType *s = &stacks[stack_id];

        if (strcmp(command, "INIT") == 0) {
            free(s->data);
            s->data = NULL;
            init_stack(s);
        } else if (strcmp(command, "PUSH") == 0) {
            if (scanf("%d", &value) != 1) {
                fprintf(stderr, "호출 코드 입력 오류: 삽입 값\n");
                return 2;
            }
            push(s, value);
        } else if (strcmp(command, "POP") == 0) {
            printf("POP %d\n", pop(s));
        } else if (strcmp(command, "PEEK") == 0) {
            printf("PEEK %d\n", peek(s));
        } else if (strcmp(command, "EMPTY") == 0) {
            printf("EMPTY %d\n", is_empty(s));
        } else if (strcmp(command, "FULL") == 0) {
            printf("FULL %d\n", is_full(s));
        } else if (strcmp(command, "TOP") == 0) {
            printf("TOP %d\n", s->top);
        } else if (strcmp(command, "CAPACITY") == 0) {
            printf("CAPACITY %d\n", s->capacity);
        } else if (strcmp(command, "DATA") == 0) {
            if (scanf("%d", &value) != 1 || value < 0 || value > s->top || value >= s->capacity) {
                fprintf(stderr, "호출 코드 입력 오류: 배열 위치\n");
                return 2;
            }
            printf("DATA %d %d\n", value, s->data[value]);
        } else {
            fprintf(stderr, "호출 코드 입력 오류: 명령\n");
            return 2;
        }
    }

    free(stacks[0].data);
    free(stacks[1].data);
    return 0;
}
