#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct {
    element *data;
    int capacity;
    int top;
} StackType;

// 초기 용량 1의 빈 스택 생성
void init_stack(StackType *s)
{
    s->top = -1;
    s->capacity = 1;
    s->data = malloc(s->capacity * sizeof(element));
    if (s->data == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
}

int is_empty(StackType *s)
{
    return (s->top == -1);
}

int is_full(StackType *s)
{
    return (s->top == s->capacity - 1);
}

// 공간이 부족하면 기존 값을 보존하면서 용량을 2배로 확장
void push(StackType *s, element item)
{
    if (is_full(s)) {
        element *data = realloc(s->data, s->capacity * 2 * sizeof(element));
        if (data == NULL) {
            fprintf(stderr, "메모리 할당 오류\n");
            exit(1);
        }
        s->data = data;
        s->capacity *= 2;
    }
    s->data[++(s->top)] = item;
}

element pop(StackType *s)
{
    if (is_empty(s)) {
        fprintf(stderr, "스택 공백 에러\n");
        exit(1);
    }
    return s->data[(s->top)--];
}

element peek(StackType *s)
{
    if (is_empty(s)) {
        fprintf(stderr, "스택 공백 에러\n");
        exit(1);
    }
    return s->data[s->top];
}
