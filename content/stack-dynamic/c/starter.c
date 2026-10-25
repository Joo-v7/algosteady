#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct {
    element *data;
    int capacity;
    int top;
} StackType;

void init_stack(StackType *s);
int is_empty(StackType *s);
int is_full(StackType *s);
void push(StackType *s, element item);
element pop(StackType *s);
element peek(StackType *s);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
